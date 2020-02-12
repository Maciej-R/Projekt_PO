import java.awt.image.AreaAveragingScaleFilter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.*;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.core.Pcaps;
import org.pcap4j.util.LinkLayerAddress;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import javax.crypto.Mac;

/**
 * Classic traceroute
 */
public class Traceroute implements Runnable {

    HashMap<String, String> parameters;
    private byte ttl = 0;
    private boolean ready = false;
    private boolean reached = false;
    private long start = 0;

    /**
     * @param parameters Command options
     */
    public Traceroute(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Execute traceroute
     */
    @Override
    public void run() {

        //Target address
        String taddrS = parameters.get("ta");
        if(taddrS == null){

            Presenter.show("Traceroute requires target address");
            return;

        }

        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(taddrS);
        }catch (Exception e){

            Presenter.show("Wrong target address");
            return;

        }

        //Result output
        StringBuilder sbuilder = new StringBuilder(100);
        sbuilder.append("Route to " + addr.toString() + "\n");

        //Find network interface to use
        PcapNetworkInterface netInter = null;
        if(parameters.get("if") == null){

            try {
                //If no interface was specified, look through available devices
                List<PcapNetworkInterface> list = Pcaps.findAllDevs();
                Iterator<PcapNetworkInterface> it = list.iterator();
                if(!it.hasNext()){Presenter.show("No network interfaces available\n"); return;}
                PcapNetworkInterface netif = it.next();
                for(; it.hasNext() && netInter == null ; it.next()){

                    if(netif.isUp()){

                        netInter = netif;

                    }

                }//None was in state "UP"
                if(netInter == null)
                    {
                        //List available interfaces to user and get choice
                        netInter = new NifSelector(){

                            public void invokeShow(List<PcapNetworkInterface> nifs){

                                try {
                                    showNifList(nifs);
                                }catch(Exception e){}

                            }

                            /*public void test(Object n){
                                System.out.println("hgurewhg");
                            }*/

                            public PcapNetworkInterface getChoice(List<PcapNetworkInterface> niflist){

                                String input = null;
                                try {
                                    input = Presenter.userInput(this, this.getClass().getMethod("invokeShow", List.class), niflist);
                                    //input = Presenter.userInput(this, this.getClass().getMethod("test", Object.class), null);
                                }catch (Exception e){}

                                int in = Integer.parseInt(input);
                                if(in < 0 || in > niflist.size()) return null;
                                return niflist.get(in);

                            }
                        }.getChoice(list);

                        //None was chosen automatically and user input wad invalid
                        if(netInter == null){

                            Presenter.show("No interface selected");
                            return;

                        }

                    }
            }catch (Exception e){System.out.println(e.toString());}

        }else{

            //Look for interface specified by user
            try {
                List<PcapNetworkInterface> list = Pcaps.findAllDevs();
                Iterator<PcapNetworkInterface> it = list.iterator();
                if(!it.hasNext()){Presenter.show("No network interfaces available\n"); return;}
                PcapNetworkInterface netif = it.next();
                for(; it.hasNext() && netInter == null ; it.next()){

                    if(netif.isUp() && netif.getName().equals(parameters.get("if"))){

                        netInter = netif;

                    }

                }
            }catch(Exception e){ Presenter.show("Wrong interface name"); }

        }

        //Networking handle
        PcapHandle handle = null;
        try {
            handle = netInter.openLive(500, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 200);
        }catch(Exception e){ Presenter.show("Cannot get network handle"); return; }

        PcapHandle sendHandle = null;
        try {
            sendHandle = netInter.openLive(500, PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS, 200);
        }catch(Exception e){ Presenter.show("Cannot get network send handle"); return; }

        /*try {
            List<DataLinkType> dataLinks = sendHandle.listDatalinks();
            for(DataLinkType dlink : dataLinks){

                System.out.println(dlink.name());

            }
            sendHandle.setDlt(DataLinkType.IEEE802_11_RADIO);
        }catch(Exception e){}*/

        //Source address
        InetAddress srcaddr = null;
        if(parameters.get("sa") == null){

            //Using address form chosen interface
            List<PcapAddress> list = netInter.getAddresses();
            Iterator<PcapAddress> it = list.iterator();
            if(!it.hasNext()){ Presenter.show("No Ip address available on interface" + netInter.toString()); return; }
            while(srcaddr == null && it.hasNext()){

                PcapAddress tmp = it.next();
                if(tmp.getAddress() instanceof Inet4Address) srcaddr = tmp.getAddress();

            }

        }else{

            //Using source address from parameters
            try{
                List<PcapAddress> list = netInter.getAddresses();
                Iterator<PcapAddress> it = list.iterator();
                if(!it.hasNext()){ Presenter.show("No Ip address available on interface" + netInter.toString()); return; }
                InetAddress sa = InetAddress.getByName(parameters.get("sa"));
                while(srcaddr == null && it.hasNext()){

                    PcapAddress tmp = it.next();
                    if(tmp.getAddress().equals(sa)) srcaddr = tmp.getAddress();

                }
                //netInter = NetworkInterface.getByName(parameters.get("sa"));
            }catch (Exception e){ Presenter.show("Wrong source Ip address"); return; }

        }

        if(srcaddr == null){

            Presenter.show("Wrong source IP");
            return;

        }

        //Icmp packet building
        IcmpV4EchoPacket.Builder echoBuilder = new IcmpV4EchoPacket.Builder();
        echoBuilder.identifier((short) 1);
        IcmpV4CommonPacket.Builder icmpBuilder = new IcmpV4CommonPacket.Builder();
        icmpBuilder.correctChecksumAtBuild(true);
        icmpBuilder.code(IcmpV4Code.NO_CODE);
        icmpBuilder.type(IcmpV4Type.ECHO);
        icmpBuilder.payloadBuilder(echoBuilder);

        //Ip packet building
        IpV4Packet.Builder packetBuilder = new IpV4Packet.Builder();
        packetBuilder.correctChecksumAtBuild(true);
        packetBuilder.dstAddr((Inet4Address)addr);
        packetBuilder.srcAddr((Inet4Address)srcaddr);
        packetBuilder.protocol(IpNumber.ICMPV4);
        packetBuilder.correctLengthAtBuild(true);
        packetBuilder.payloadBuilder(icmpBuilder);
        packetBuilder.tos(IpV4Rfc1349Tos.newInstance((byte)1));
        packetBuilder.version(IpVersion.IPV4);

        //Capture icmp response
        String srcaddrS = srcaddr.toString();
        srcaddrS = srcaddrS.substring(1, srcaddrS.length());
        try {
            handle.setFilter("icmp and ip dst " + srcaddrS, BpfProgram.BpfCompileMode.NONOPTIMIZE);
        }catch (Exception e){ Presenter.show("Cannot properly filter incoming traffic"); return; }

        //Building ethernet packet
        EthernetPacket.Builder ethBuilder = new EthernetPacket.Builder();
        ethBuilder.payloadBuilder(packetBuilder);
        //Finding local MacAddress
        MacAddress macaddr = null;
        List<LinkLayerAddress> DLadresses = netInter.getLinkLayerAddresses();
        for(LinkLayerAddress dladdr : DLadresses){

            if(dladdr instanceof MacAddress){

                macaddr = MacAddress.getByAddress(dladdr.getAddress());
                ethBuilder.srcAddr(macaddr);
                break;

            }

        }
        ethBuilder.paddingAtBuild(true);
        ethBuilder.type(EtherType.IPV4);
        if(parameters.get("gw") == null) {
            ethBuilder.dstAddr(resolveGateway(netInter));
        }else
            try {
                ethBuilder.dstAddr(sendARP(InetAddress.getByName(parameters.get("gw")), srcaddr, macaddr, netInter));
            }catch(Exception e) {return;}

        byte maxttl = parameters.get("mttl") == null ? 127 : Byte.parseByte(parameters.get("mttl"));

        //Results list
        List<EchoReplayStats> list = new LinkedList<>();
        //Start listening to traffic
        (new Thread(new Capture(handle, addr, list, this, maxttl))).start();

        //Traceroute
        for(ttl = 1; ttl < maxttl; ++ttl) {

            //Send packet
            packetBuilder.ttl(ttl);


            EthernetPacket eth = ethBuilder.build();

            start = System.currentTimeMillis();
            try {
                sendHandle.sendPacket(eth);
            } catch (Exception e) {
                Presenter.show("Error sending packet"); return;
            }

            //Wait until response is captured or call is timed out
            long waitStart = System.currentTimeMillis();
            while(!ready){

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                if(System.currentTimeMillis() - waitStart > 10000) {

                    list.add(new EchoReplayStats(ttl, null, 10000));
                    break;

                }

            }

            ready = false;

            if(reached) break;

        }

        list.sort((EchoReplayStats stat1, EchoReplayStats stat2) -> Integer.compare(stat1.ttl, stat2.ttl));

        //Build results to display
        for(EchoReplayStats st : list){

            if(st.source == null) {
                sbuilder.append(String.format("\t%d  %s  %dms\n", st.ttl, "Timed out", st.duration));
                continue;
            }
            sbuilder.append(String.format("\t%d  %s  %dms\n", st.ttl, st.source.toString(), st.duration));

        }

        Presenter.show(sbuilder.toString());

        handle.close();
        sendHandle.close();

    }

    private class Capture implements Runnable{

        PcapHandle handle;
        InetAddress target;
        List<EchoReplayStats> list;
        Traceroute tr;
        byte toCatch;

        Capture(PcapHandle h, InetAddress a, List<EchoReplayStats> l, Traceroute t, byte toCatch){

            handle = h;
            target = a;
            list = l;
            tr = t;
            this.toCatch = toCatch;

        }

        @Override
        public void run() {

            PacketListener listener = new PacketListener() {
                @Override
                public void gotPacket(Packet packet) {

                    //Listener works until amount of frames given at time of construction is captured
                    if(tr.reached()) return;

                    //Duration of call
                    long start = tr.sentTime();
                    long end = System.currentTimeMillis();

                    //IP packet from Ethernet frame
                    IpV4Packet pck = (IpV4Packet)packet.getPayload();
                    //Source address from Ip header
                    InetAddress reached = pck.getHeader().getSrcAddr();

                    //Add result
                    list.add(new EchoReplayStats(tr.currttl(), reached, end - start));

                    if(reached.equals(target)) tr.signalTargetReached();

                    tr.signalDone();

                }
            };

            try {
                handle.loop(toCatch, listener);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private class EchoReplayStats{

        public int ttl;
        public InetAddress source;
        public long duration;

        public EchoReplayStats(int ttl, InetAddress source, long duration) {
            this.ttl = ttl;
            this.source = source;
            this.duration = duration;
        }
    }

    public byte currttl(){

        return ttl;

    }

    public void signalDone(){

        ready = true;

    }

    public void signalTargetReached(){

        reached = true;

    }

    public boolean reached(){

        return reached;

    }

    public long sentTime(){

        return start;

    }

    /**
     * Due to lack of possibility to get gateway information in Java and no support for DHCP
     * only possibility is to listen to traffic on interface and get MacAddress form captured packets
     * Getting IP address is possible by sending than RARP packet
     * DOESN'T give certain result - captured packet may have been initiated in local network
     * @param nif Network interface to use
     * @return MacAddress of possible gateway at given interface
     */
    @Nullable
    private MacAddress resolveGateway(PcapNetworkInterface nif){

        PcapHandle handle = null;

        try {
            handle = nif.openLive(500, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 200);
        }catch(Exception e){ Presenter.show("Cannot get network handle"); return null; }

        try {//Filer IPv4 traffic
            handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);
        }catch(Exception e){}

        MacAddressWrapper wrapper = new MacAddressWrapper();
        wrapper.macAddress = null;
        PacketListener listener = new myPacketListener(wrapper);

        //Check if capture was successful
        while(wrapper.macAddress == null){

            try {
                handle.loop(1, listener);
            }catch(Exception e){

                Presenter.show("Capture error");
                return null;

            }

        }

        return wrapper.macAddress;

    }

    //Used to get result from void call in PacketListener
    private class MacAddressWrapper{

        public MacAddress macAddress;

    }

    /**
     * Sets MacAddress in wrapper given in constructor argument to
     * one with witch is communication on interface where it is set
     */
    private class myPacketListener implements PacketListener{

        MacAddressWrapper wrapper;

        myPacketListener(MacAddressWrapper wrp){

            wrapper = wrp;

        }

        @Override
        public void gotPacket(Packet packet) {

            //Check that type is IPv4
            if(((packet.getPayload().getHeader().getRawData()[0] >> 4 ) != (byte)4)) return;

            //Ip packet from Ethernet
            IpV4Packet IpPck = (IpV4Packet)packet.getPayload();

            //Addresses from header
            InetAddress src = IpPck.getHeader().getSrcAddr();
            InetAddress dst = IpPck.getHeader().getDstAddr();

            //byte srcb[] = src.getAddress();
            //byte dstb[] = dst.getAddress();

            //Direction of transmitting frame
            boolean incoming = true;
            //Check witch address is local
            NetworkInterface locif = null;
            try {
                locif = NetworkInterface.getByInetAddress(dst);
            }catch(Exception e){  }

            if(locif == null){

                try {
                    locif = NetworkInterface.getByInetAddress(src);
                }catch(Exception e){ return; }
                incoming = false;

            }

            /*Check if local network, no way to differentiate gateway form others
            List<InterfaceAddress> list = locif.getInterfaceAddresses();
            Iterator<InterfaceAddress> it = list.iterator();
            if(!it.hasNext()) return;

            int mask_len = 0;
            for(InterfaceAddress addr = it.next(); it.hasNext(); addr = it.next() ){

                if(addr.getAddress() != dst) continue;

                mask_len = addr.getNetworkPrefixLength();
                break;

            }

            int srci = Algorithm.btoi(srcb);
            int dsti = Algorithm.btoi(dstb);

            if((srci & (int)(Math.pow(2, mask_len) - 1)) == (dsti & (int)(Math.pow(2, mask_len) - 1))) return;*/

            //Set value to address of appropriate one from Ethernet header
            if(incoming)
                wrapper.macAddress = ((EthernetPacket.EthernetHeader)packet.getHeader()).getSrcAddr();
            else
                wrapper.macAddress = ((EthernetPacket.EthernetHeader)packet.getHeader()).getDstAddr();

        }
    }

    private MacAddress sendARP(InetAddress resolve, InetAddress sender, MacAddress mac, PcapNetworkInterface netif){

        //if(!(addr instanceof Inet4Address)) return null;

        ArpPacket.Builder arpBuilder = new ArpPacket.Builder();
        arpBuilder.dstProtocolAddr(resolve);
        arpBuilder.srcProtocolAddr(sender);
        arpBuilder.hardwareAddrLength((byte)6);
        arpBuilder.hardwareType(ArpHardwareType.ETHERNET);
        arpBuilder.srcHardwareAddr(mac);
        arpBuilder.dstHardwareAddr(MacAddress.getByName("00:00:00:00:00:00"));
        arpBuilder.operation(ArpOperation.REQUEST);
        arpBuilder.protocolType(EtherType.IPV4);
        arpBuilder.protocolAddrLength((byte)4);

        EthernetPacket.Builder ethBuilder = new EthernetPacket.Builder();
        ethBuilder.type(EtherType.ARP);
        ethBuilder.paddingAtBuild(true);
        ethBuilder.payloadBuilder(arpBuilder);
        ethBuilder.srcAddr(mac);
        ethBuilder.dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS);

        PcapHandle handle;
        try {
            handle = netif.openLive(500, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 200);
            handle.setFilter("arp", BpfProgram.BpfCompileMode.OPTIMIZE);
        }catch(Exception e){ return null; }

        MacAddressWrapper res = new MacAddressWrapper();
        res.macAddress = null;
        while(res.macAddress == null){

            try {
                handle.sendPacket(ethBuilder.build());
                handle.loop(1, new ArpCatcher(res, resolve));
            }catch(Exception e){ return null; }

        }

        return res.macAddress;

    }

    private class ArpCatcher implements PacketListener{

        MacAddressWrapper wrapper;
        InetAddress src;

        ArpCatcher(MacAddressWrapper wrapper, InetAddress src){

            this.wrapper = wrapper;
            this.src = src;

        }

        @Override
        public void gotPacket(Packet packet) {

            ArpPacket arpPck = (ArpPacket) packet.getPayload();
            if(!arpPck.getHeader().getSrcProtocolAddr().equals(src)) return;
            wrapper.macAddress = arpPck.getHeader().getSrcHardwareAddr();

        }

    }

}