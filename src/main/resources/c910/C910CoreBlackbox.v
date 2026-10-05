// Chipyard tile wrapper for the openC910 cluster: one core, one 128-bit AXI4 master.
// Port names follow Chisel's flattening of rocket-chip's AXI4Bundle (C910Tile.scala).
// The open-source cluster always has two cores; core 1 is held in reset, as in smart_run.
// The cluster's own PLIC and CLINT sit at apb_base (0xb0000000), CLINT at +0x4000000.
module C910CoreBlackbox (
  input          clock,
  input          reset,
  input  [39:0]  reset_vector,
  input  [2:0]   hartid,

  input          axi_aw_ready,
  output         axi_aw_valid,
  output [7:0]   axi_aw_bits_id,
  output [39:0]  axi_aw_bits_addr,
  output [7:0]   axi_aw_bits_len,
  output [2:0]   axi_aw_bits_size,
  output [1:0]   axi_aw_bits_burst,
  output         axi_aw_bits_lock,
  output [3:0]   axi_aw_bits_cache,
  output [2:0]   axi_aw_bits_prot,
  output [3:0]   axi_aw_bits_qos,

  input          axi_w_ready,
  output         axi_w_valid,
  output [127:0] axi_w_bits_data,
  output [15:0]  axi_w_bits_strb,
  output         axi_w_bits_last,

  output         axi_b_ready,
  input          axi_b_valid,
  input  [7:0]   axi_b_bits_id,
  input  [1:0]   axi_b_bits_resp,

  input          axi_ar_ready,
  output         axi_ar_valid,
  output [7:0]   axi_ar_bits_id,
  output [39:0]  axi_ar_bits_addr,
  output [7:0]   axi_ar_bits_len,
  output [2:0]   axi_ar_bits_size,
  output [1:0]   axi_ar_bits_burst,
  output         axi_ar_bits_lock,
  output [3:0]   axi_ar_bits_cache,
  output [2:0]   axi_ar_bits_prot,
  output [3:0]   axi_ar_bits_qos,

  output         axi_r_ready,
  input          axi_r_valid,
  input  [7:0]   axi_r_bits_id,
  input  [127:0] axi_r_bits_data,
  input  [1:0]   axi_r_bits_resp,
  input          axi_r_bits_last
);

wire rst_b = !reset;

// mtime source for the internal CLINT and the time CSR.
reg [63:0] sys_cnt;
always @(posedge clock or negedge rst_b)
  if (!rst_b)
    sys_cnt <= 64'b0;
  else
    sys_cnt <= sys_cnt + 64'b1;

assign axi_aw_bits_qos = 4'b0;
assign axi_ar_bits_qos = 4'b0;

openC910 x_openC910 (
  .pll_cpu_clk                  (clock),
  .axim_clk_en                  (1'b1),
  .pad_cpu_rst_b                (rst_b),
  .pad_yy_dft_clk_rst_b         (rst_b),
  .pad_cpu_apb_base             (40'hb0000000),
  .pad_cpu_sys_cnt              (sys_cnt),
  .pad_cpu_l2cache_flush_req    (1'b0),
  .pad_plic_int_cfg             (144'b0),
  .pad_plic_int_vld             (144'b0),

  .pad_core0_rst_b              (rst_b),
  .pad_core0_rvba               (reset_vector),
  .pad_core0_hartid             (hartid),
  .pad_core0_dbg_mask           (1'b0),
  .pad_core0_dbgrq_b            (1'b1),
  .pad_core1_rst_b              (1'b0),
  .pad_core1_rvba               (40'b0),
  .pad_core1_hartid             (3'h1),
  .pad_core1_dbg_mask           (1'b0),
  .pad_core1_dbgrq_b            (1'b1),

  .pad_had_jtg_tclk             (1'b0),
  .pad_had_jtg_tdi              (1'b0),
  .pad_had_jtg_tms              (1'b0),
  .pad_had_jtg_trst_b           (rst_b),

  .pad_l2c_data_mbist_clk_ratio (3'b0),
  .pad_l2c_tag_mbist_clk_ratio  (3'b0),
  .pad_yy_icg_scan_en           (1'b0),
  .pad_yy_mbist_mode            (1'b0),
  .pad_yy_scan_enable           (1'b0),
  .pad_yy_scan_mode             (1'b0),
  .pad_yy_scan_rst_b            (1'b1),
  .pad_biu_csysreq              (1'b0),

  .biu_pad_awvalid              (axi_aw_valid),
  .pad_biu_awready              (axi_aw_ready),
  .biu_pad_awid                 (axi_aw_bits_id),
  .biu_pad_awaddr               (axi_aw_bits_addr),
  .biu_pad_awlen                (axi_aw_bits_len),
  .biu_pad_awsize               (axi_aw_bits_size),
  .biu_pad_awburst              (axi_aw_bits_burst),
  .biu_pad_awlock               (axi_aw_bits_lock),
  .biu_pad_awcache              (axi_aw_bits_cache),
  .biu_pad_awprot               (axi_aw_bits_prot),

  .biu_pad_wvalid               (axi_w_valid),
  .pad_biu_wready               (axi_w_ready),
  .biu_pad_wdata                (axi_w_bits_data),
  .biu_pad_wstrb                (axi_w_bits_strb),
  .biu_pad_wlast                (axi_w_bits_last),

  .pad_biu_bvalid               (axi_b_valid),
  .biu_pad_bready               (axi_b_ready),
  .pad_biu_bid                  (axi_b_bits_id),
  .pad_biu_bresp                (axi_b_bits_resp),

  .biu_pad_arvalid              (axi_ar_valid),
  .pad_biu_arready              (axi_ar_ready),
  .biu_pad_arid                 (axi_ar_bits_id),
  .biu_pad_araddr               (axi_ar_bits_addr),
  .biu_pad_arlen                (axi_ar_bits_len),
  .biu_pad_arsize               (axi_ar_bits_size),
  .biu_pad_arburst              (axi_ar_bits_burst),
  .biu_pad_arlock               (axi_ar_bits_lock),
  .biu_pad_arcache              (axi_ar_bits_cache),
  .biu_pad_arprot               (axi_ar_bits_prot),

  .pad_biu_rvalid               (axi_r_valid),
  .biu_pad_rready               (axi_r_ready),
  .pad_biu_rid                  (axi_r_bits_id),
  .pad_biu_rdata                (axi_r_bits_data),
  .pad_biu_rresp                (axi_r_bits_resp),
  .pad_biu_rlast                (axi_r_bits_last)
);

endmodule
