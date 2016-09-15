# -*- mode: ruby -*-
# vi: set ft=ruby :
Vagrant.configure(2) do |config|
  config.vm.box = "aerospike/centos-6.5"

  config.vm.provider :virtualbox do |vb|
    vb.memory = '2048'
    vb.cpus = 2
  end

  config.ssh.insert_key = false
end
