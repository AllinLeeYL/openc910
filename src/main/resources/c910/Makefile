# Concatenate the C910 RTL into one file for Chipyard's Verilator flow (run by C910Tile.scala).
# sysmap.h here replaces gen_rtl/mmu/rtl/sysmap.h; the RTL itself is used unchanged.
rtl_dir := $(abspath ../../../../C910_RTL_FACTORY)
vsrcs := $(shell sed -n 's#^$${CODE_BASE_PATH}\(.*\.v\)$$#$(rtl_dir)\1#p' $(rtl_dir)/gen_rtl/filelists/C910_asic_rtl.fl)

C910CoreBlackbox.preprocessed.v: $(rtl_dir)/gen_rtl/cpu/rtl/cpu_cfig.h sysmap.h $(vsrcs) C910CoreBlackbox.v
	awk 1 $^ > $@

# The boot ROM image is checked in so Chipyard needs no RISC-V toolchain; rebuild with `make bootrom.img`.
bootrom.img: bootrom.S
	riscv64-unknown-elf-gcc -march=rv64i_zicsr -mabi=lp64 -nostdlib -static -Ttext=0x10000 $< -o bootrom.elf
	riscv64-unknown-elf-objcopy -O binary bootrom.elf $@
	rm bootrom.elf
