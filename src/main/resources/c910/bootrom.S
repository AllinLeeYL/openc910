// C910 boot ROM, the reset vector (0x10000). With +loadmem the program is already
// in DRAM, so unlike testchipip's bootrom it does not wait for an MSIP wake-up
// (Chipyard's CLINT is not wired to C910, which has its own).
#define CSR_MXSTATUS 0x7c0
#define CSR_MHCR     0x7c1
#define CSR_MCOR     0x7c2
#define CSR_MCCR2    0x7c3
#define CSR_MHINT    0x7c5
#define BOOTADDR_REG 0x1000

.globl _start
_start:
  // Behave like standard RISC-V, as Spike does: clear THEADISAEE (T-Head custom
  // instructions), MAEE (PTE attribute bits) and MM (misaligned accesses complete).
  li t0, (1 << 22) | (1 << 21) | (1 << 15)
  csrc CSR_MXSTATUS, t0
  // Invalidate, then enable caches and branch prediction, as smart_run's crt0.s.
  li t0, 0x70013
  csrs CSR_MCOR, t0
  li t0, 0x11ff
  csrs CSR_MHCR, t0
  li t0, 0x6e30c
  csrs CSR_MHINT, t0
  li t0, 0xe0000009
  csrs CSR_MCCR2, t0
  li t0, BOOTADDR_REG
  ld t0, 0(t0)
  jr t0
