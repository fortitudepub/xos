ovs-vsctl del-br cluster-br
ovs-vsctl add-br cluster-br
ip link set cluster-br up
ip address add 1.1.1.1/24 dev cluster-br

ip netns del member1
ip netns add member1
ip netns exec member1 ip link set lo up
ip link add dev cluster-if1 type veth peer name member1-if1
ip link set member1-if1 netns member1
ip link set cluster-if1 up
ip netns exec member1 ip link set member1-if1 up
ip netns exec member1 ip address add 1.1.1.2/24 dev member1-if1
ovs-vsctl add-port cluster-br cluster-if1


ip netns del member2
ip netns add member2
ip netns exec member2 ip link set lo up
ip link add dev cluster-if2 type veth peer name member2-if1
ip link set member2-if1 netns member2
ip link set cluster-if2 up
ip netns exec member2 ip link set member2-if1 up
ip netns exec member2 ip address add 1.1.1.3/24 dev member2-if1
ovs-vsctl add-port cluster-br cluster-if2


# ip netns del member3
# ip netns add member3
# ip netns exec member3 ip link set lo up
# ip link add dev cluster-if3 type veth peer name member3-if1
# ip link set member3-if1 netns member3
# ip link set cluster-if3 up
# ip netns exec member3 ip link set member3-if1 up
# ip netns exec member3 ip address add 1.1.1.4/24 dev member3-if1
# ovs-vsctl add-port cluster-br cluster-if3
