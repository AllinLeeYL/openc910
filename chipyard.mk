# Ingested by Chipyard's common.mk: regenerate when the C910 RTL or wrapper changes.
EXTRA_GENERATOR_REQS += $(call lookup_srcs,$(base_dir)/generators/openc910/C910_RTL_FACTORY,v) \
	$(filter-out %.preprocessed.v,$(wildcard $(base_dir)/generators/openc910/src/main/resources/c910/*))
