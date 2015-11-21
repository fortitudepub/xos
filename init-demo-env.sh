# TODO: later we can abstract some function to make code more elegant. 

# create bridges used in demo.
ovs-vsctl del-br controller-br
ovs-vsctl add-br controller-br
ovs-vsctl del-br ofsw
ovs-vsctl add-br ofsw
ovs-vsctl del-br l2sw
ovs-vsctl add-br l2sw

# set up ip for controller listening.

# controller1 will be put in default ns and use 1.1.1.1.
ip link set controller-br up
ip address add 1.1.1.1/24 dev controller-br

# controller2 will be put in controller2 ns and have veth pair plugged in controller-br
ip netns del controller2
ip netns add controller2
ip netns exec controller2 ip link set lo up
ip link add dev controller-if2 type veth peer name controller2-if1
ip link set controller2-if1 netns controller2
ip link set controller-if2 up
ip netns exec controller2 ip link set controller2-if1 up
ip netns exec controller2 ip address add 1.1.1.2/24 dev controller2-if1
ovs-vsctl add-port controller-br controller-if2

# controller3 will be put in controller3 ns and have veth pair plugged in controller-br
ip netns del controller3
ip netns add controller3
ip netns exec controller3 ip link set lo up
ip link add dev controller-if3 type veth peer name controller3-if1
ip link set controller3-if1 netns controller3
ip link set controller-if3 up
ip netns exec controller3 ip link set controller3-if1 up
ip netns exec controller3 ip address add 1.1.1.3/24 dev controller3-if1
ovs-vsctl add-port controller-br controller-if3

# set up ofsw and l2sw and the interconnect link.
ip link add dev ofsw-l2sw-if type veth peer name l2sw-ofsw-if
ip link set ofsw-l2sw-if up
ip link set l2sw-ofsw-if up
ovs-vsctl add-port ofsw ofsw-l2sw-if
ovs-vsctl add-port l2sw l2sw-ofsw-if

# set up client ns and veth pair, and plug the if to l2sw.
ip netns del client
ip netns add client
ip link add dev client-l2sw-if1 type veth peer name l2sw-client-if1
ip link set l2sw-client-if1 up
ip link set client-l2sw-if1 netns client
ip netns exec client ip link set client-l2sw-if1 up
ip netns exec client ip address add 28.1.1.2/24  dev client-l2sw-if1
ip netns exec client ip route add 188.188.188.188/32 via 28.1.1.1 dev client-l2sw-if1
ovs-vsctl add-port l2sw l2sw-client-if1

# set up host1 ns and veth pair, and plug the if to l2sw.
ip netns del host1
ip netns add host1
ip link add dev host1-l2sw-if1 type veth peer name l2sw-host1-if1
ip link set l2sw-host1-if1 up
ip link set host1-l2sw-if1 netns host1
ip netns exec host1 ip link set host1-l2sw-if1 up
ip netns exec host1 ip address add 18.1.1.2/24  dev host1-l2sw-if1
ip netns exec host1 ip route add 0.0.0.0/0 via 18.1.1.1 dev host1-l2sw-if1
ovs-vsctl add-port l2sw l2sw-host1-if1
# set lo0 as 188.188.188.188/32 anycast address.
ip netns exec host1 ip link set lo up
ip netns exec host1 ip address add 188.188.188.188/32 dev lo

# set up host2 ns and veth pair, and plug the if to l2sw.
ip netns del host2
ip netns add host2
ip link add dev host2-l2sw-if1 type veth peer name l2sw-host2-if1
ip link set l2sw-host2-if1 up
ip link set host2-l2sw-if1 netns host2
ip netns exec host2 ip link set host2-l2sw-if1 up
ip netns exec host2 ip address add 18.1.1.3/24  dev host2-l2sw-if1
ip netns exec host2 ip route add 0.0.0.0/0 via 18.1.1.1 dev host2-l2sw-if1
ovs-vsctl add-port l2sw l2sw-host2-if1
# set lo0 as 188.188.188.188/32 anycast address.
ip netns exec host2 ip link set lo up
ip netns exec host2 ip address add 188.188.188.188/32 dev lo
