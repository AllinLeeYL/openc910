// Chipyard tile for the openC910 cluster (C910_RTL_FACTORY), after cva6-wrapper's CVA6Tile.
// The cluster keeps its own L2, PLIC and CLINT; the tile only exposes its AXI4 master.

package c910

import sys.process._

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.amba.axi4._

// Describes the core to the DTS and the rest of rocket-chip; the RTL is fixed.
case class C910CoreParams(bootFreqHz: BigInt = BigInt(1000000000)) extends CoreParams {
  val xLen: Int = 64
  val pgLevels: Int = 3
  val useVM: Boolean = true
  val useHypervisor: Boolean = false
  val useUser: Boolean = true
  val useSupervisor: Boolean = true
  val useDebug: Boolean = false
  val useAtomics: Boolean = true
  val useAtomicsOnlyForIO: Boolean = false
  val useCompressed: Boolean = true
  override val useVector: Boolean = false
  val useSCIE: Boolean = false
  val useRVE: Boolean = false
  val mulDiv: Option[MulDivParams] = Some(MulDivParams())
  val fpu: Option[FPUParams] = Some(FPUParams())
  val nLocalInterrupts: Int = 0
  val useNMI: Boolean = false
  val nPMPs: Int = 8
  val pmpGranularity: Int = 4096
  val nBreakpoints: Int = 0
  val useBPWatch: Boolean = false
  val mcontextWidth: Int = 0
  val scontextWidth: Int = 0
  val nPerfCounters: Int = 16
  val haveBasicCounters: Boolean = true
  val haveFSDirty: Boolean = false
  val misaWritable: Boolean = false
  val haveCFlush: Boolean = false
  val nL2TLBEntries: Int = 1024
  val nL2TLBWays: Int = 1
  val mtvecInit: Option[BigInt] = Some(BigInt(0))
  val mtvecWritable: Boolean = true
  val instBits: Int = 16
  val lrscCycles: Int = 80
  val decodeWidth: Int = 3
  val fetchWidth: Int = 4
  val retireWidth: Int = 3
  val nPTECacheEntries: Int = 0
  val traceHasWdata: Boolean = false
  val useConditionalZero: Boolean = false
  val useZba: Boolean = false
  val useZbb: Boolean = false
  val useZbs: Boolean = false
}

case class C910TileAttachParams(
  tileParams: C910TileParams,
  crossingParams: RocketCrossingParams
) extends CanAttachTile {
  type TileType = C910Tile
  val lookup = PriorityMuxHartIdFromSeq(Seq(tileParams))
}

case class C910TileParams(
  name: Option[String] = Some("c910_tile"),
  tileId: Int = 0,
  core: C910CoreParams = C910CoreParams()
) extends InstantiableTileParams[C910Tile] {
  val beuAddr: Option[BigInt] = None
  val blockerCtrlAddr: Option[BigInt] = None
  val btb: Option[BTBParams] = Some(BTBParams())
  val dcache: Option[DCacheParams] = Some(DCacheParams())
  val icache: Option[ICacheParams] = Some(ICacheParams())
  val clockSinkParams: ClockSinkParameters = ClockSinkParameters()
  def instantiate(crossing: HierarchicalElementCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters): C910Tile =
    new C910Tile(this, crossing, lookup)
  val baseName = name.getOrElse("c910_tile")
  val uniqueName = s"${baseName}_$tileId"
}

class C910Tile private(
  val c910Params: C910TileParams,
  crossing: ClockCrossingType,
  lookup: LookupByHartIdImpl,
  q: Parameters)
  extends BaseTile(c910Params, crossing, lookup, q)
  with SinksExternalInterrupts
  with SourcesExternalNotifications
{
  def this(params: C910TileParams, crossing: HierarchicalElementCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters) =
    this(params, crossing.crossingType, lookup, p)

  val intOutwardNode = None
  val slaveNode = TLIdentityNode()
  val masterNode = visibilityNode

  tlOtherMastersNode := tlMasterXbar.node
  masterNode :=* tlOtherMastersNode
  DisableMonitors { implicit p => tlSlaveXbar.node :*= slaveNode }

  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("thead,c910", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping ++ cpuProperties ++ nextLevelCacheProperty ++ tileProperties)
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(tileId))
  }

  override lazy val module = new C910TileModuleImp(this)

  // C910's BIU: 128-bit data, 8-bit IDs, bursts of up to one 64-byte cache line.
  val memAXI4Node = AXI4MasterNode(Seq(AXI4MasterPortParameters(
    masters = Seq(AXI4MasterParameters(name = "c910-mem-port-axi4", id = IdRange(0, 1 << 8))))))

  (tlMasterXbar.node
    := TLBuffer()
    := TLFIFOFixer(TLFIFOFixer.all)
    := TLWidthWidget(16)
    := AXI4ToTL()
    := AXI4UserYanker(Some(2))
    := AXI4Fragmenter()
    := memAXI4Node)
}

class C910TileModuleImp(outer: C910Tile) extends BaseTileModuleImp(outer) {
  val core = Module(new C910CoreBlackbox)

  core.io.clock := clock
  core.io.reset := reset.asBool
  core.io.reset_vector := outer.resetVectorSinkNode.bundle
  core.io.hartid := outer.hartIdSinkNode.bundle

  // The cluster has its own PLIC and CLINT, so the subsystem's interrupts stay unused.
  outer.traceSourceNode.bundle := DontCare
  outer.traceSourceNode.bundle.insns.foreach(_.valid := false.B)

  // ponytail: C910's 40-bit addresses are truncated to the system bus width, so accesses
  // above it alias low memory instead of faulting, as in cva6-wrapper.
  outer.memAXI4Node.out.foreach { case (out, _) =>
    out.aw <> core.io.axi.aw
    out.w  <> core.io.axi.w
    out.ar <> core.io.axi.ar
    core.io.axi.b <> out.b
    core.io.axi.r <> out.r
  }
}

class C910CoreBlackbox extends BlackBox with HasBlackBoxPath {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val reset_vector = Input(UInt(40.W))
    val hartid = Input(UInt(3.W))
    val axi = new AXI4Bundle(AXI4BundleParameters(addrBits = 40, dataBits = 128, idBits = 8))
  })

  // Concatenate the RTL, with the Chipyard sysmap.h, into one file (see the Makefile).
  val vsrcDir = s"${System.getProperty("user.dir")}/generators/openc910/src/main/resources/c910"
  require(Seq("make", "-C", vsrcDir, "C910CoreBlackbox.preprocessed.v").! == 0, "Failed to preprocess the C910 RTL")
  addPath(s"$vsrcDir/C910CoreBlackbox.preprocessed.v")
}

class WithNC910Cores(n: Int = 1) extends Config((site, here, up) => {
  case TilesLocated(InSubsystem) => {
    val prev = up(TilesLocated(InSubsystem))
    val idOffset = up(NumTiles)
    (0 until n).map { i =>
      C910TileAttachParams(
        tileParams = C910TileParams(tileId = i + idOffset),
        crossingParams = RocketCrossingParams())
    } ++ prev
  }
  case NumTiles => up(NumTiles) + n
})

// Boots straight into DRAM after enabling C910's caches; see src/main/resources/c910/bootrom.S.
class WithC910BootROM extends Config((site, here, up) => {
  case BootROMLocated(x) => up(BootROMLocated(x))
    .map(_.copy(contentFileName = ResourceFileName("/c910/bootrom.img")))
})
