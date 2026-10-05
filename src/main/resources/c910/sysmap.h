// Chipyard memory map for C910, replacing gen_rtl/mmu/rtl/sysmap.h.
// Region i covers [BASE_ADDR(i-1), BASE_ADDR(i)) in 4 KiB pages (addr[39:12]).
// Flags {SO, C, B, SH, SEC}: 5'b10000 strongly ordered, 5'b01111 cacheable.
//
// The pages holding tohost/fromhost stay uncached so the host (fesvr over TSI)
// sees the core's writes; C910's write-back L2 is not coherent with the system bus.
// rvgen gives each its own 4 KiB page at the top of its RAM (default 0x80000000
// + 128 MiB), so no other data is uncached: C910 faults AMOs to uncached memory.

// debug, boot-address register, error device
`define SYSMAP_BASE_ADDR0  28'h00010
`define SYSMAP_FLG0        5'b10000
// boot ROM
`define SYSMAP_BASE_ADDR1  28'h00020
`define SYSMAP_FLG1        5'b01111
// CLINT, PLIC, scratchpad, UART and other MMIO
`define SYSMAP_BASE_ADDR2  28'h80000
`define SYSMAP_FLG2        5'b10000
// DRAM
`define SYSMAP_BASE_ADDR3  28'h87ffe
`define SYSMAP_FLG3        5'b01111
// fromhost and tohost pages
`define SYSMAP_BASE_ADDR4  28'h88000
`define SYSMAP_FLG4        5'b10000
// rest of the 256 MiB DRAM
`define SYSMAP_BASE_ADDR5  28'h90000
`define SYSMAP_FLG5        5'b01111
// unmapped, and C910's internal PLIC/CLINT at 0xb0000000
`define SYSMAP_BASE_ADDR6  28'hfffffff
`define SYSMAP_FLG6        5'b10000
`define SYSMAP_BASE_ADDR7  28'hfffffff
`define SYSMAP_FLG7        5'b10000
