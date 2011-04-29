
println "octopus.groovy: ${revision}"
//assert !revision.moving
//println "path: ${env.PATH}"

//build by maven: this ensure correct preparation
build = maven()

artifact jar(groupId:'eu.lmc.tools', artifactId:'applyalter', classifier:'jar-with-dependencies')

