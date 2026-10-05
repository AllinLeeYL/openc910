package chipyard

import org.chipsalliance.cde.config.{Config}

// ---------------------
// C910 Configs (from the openc910 generator)
// ---------------------

// One C910 core; the cluster's second core is held in reset.
class C910Config extends Config(
  new c910.WithC910BootROM ++
  new c910.WithNC910Cores(1) ++
  new chipyard.config.AbstractConfig)
