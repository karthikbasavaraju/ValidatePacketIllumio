import java.io.File;
import java.util.*;

public class Illumio {

    final int limit=5;
    List<TreeMap> nodes;

    //Object to hold all the parameters of a packet
    private class IPAddress{

        Boolean isTcp,isInbound;
        Integer[][] ipPortRange;            //to hold range of port and ip values

        IPAddress(String[] packetParameters){

            isInbound = !packetParameters[0].equals("outbound");
            isTcp = packetParameters[1].equals("tcp");

            setRange(packetParameters);
        }

        int getVal(int i,int j){
            return ipPortRange[i][j];
        }


        //To get range of port and IP
        void setRange(String[] packetParameters){

            ipPortRange = new Integer[2][limit];

            if(packetParameters[2].contains("-")) {
                String[] temp = packetParameters[2].split("-");
                findRangeOf(temp[0], ipPortRange, 0,0);
                findRangeOf(temp[1],ipPortRange,1,0);
            }
            else
            {
                findRangeOf(packetParameters[2],ipPortRange,0,0);
                findRangeOf(packetParameters[2],ipPortRange,1,0);
            }

            if(packetParameters[3].contains("-")){
                String[] temp = packetParameters[3].split("-");
                findRangeOf(temp[0],ipPortRange,0,1);
                findRangeOf(temp[1],ipPortRange,1,1);
            }
            else
            {
                findRangeOf(packetParameters[3],ipPortRange,0,1);
                findRangeOf(packetParameters[3],ipPortRange,1,1);
            }
        }

        void findRangeOf(String str, Integer[][] range, int ind,int j){

            for(int i=0,n=str.length(),num=0; i<n ; i++,num=0,j++){
                while(i<n && str.charAt(i)!='.'){
                    num = num*10 + (str.charAt(i++)-'0');
                }
                range[ind][j] = num;
            }
        }
    }


    Illumio(String path){
        nodes = new LinkedList<>();
        for(int i=0;i<4;i++)
            nodes.add(new TreeMap());
        readRules(path);
    }

    //to read rules from csv file and to converts rules into tree with non-overlapping range parameters as nodes
    private void readRules(String path){

        try {
            Scanner scanner = new Scanner(new File(path));
            while (scanner.hasNextLine()) {
                IPAddress temp = new IPAddress(scanner.nextLine().split(","));
                buildTree(temp, 0, getNode(temp.isInbound, temp.isTcp),true);
            }
            //to print pictorial representation of my rule arrangement
            //printRules();
            scanner.close();
        }
        catch (Exception e){
            System.out.println("Input format is not correct");
        }
    }

    //testing- prints the tree
    private void printRules(){
        int i=0;
        for(TreeMap<Range,TreeMap> t : nodes){
            System.out.println((i++)+":");
            dfs(t,"");
        }

    }

    //testing - prints the tree
    private void dfs(TreeMap<Range,TreeMap> t,String s){
        for(Range r : t.keySet()){
            System.out.println(s+r.start+"-"+r.end+",");
            dfs(t.get(r),s+"\t");
        }
    }


    //To add new rule into tree
    private void addNewRules(IPAddress ip, TreeMap<Range,TreeMap> curNode, Map<Range,TreeMap> temp, int i){
        for(Range curRange : temp.keySet())
        {
            buildTree(ip,i+1,curNode.get(curRange),false);
        }
    }

    //When two rules cross each other, we have to split them into two non-overlapping rules
    private void splitEdges(Range l, Range r, TreeMap<Range,TreeMap> curNode,IPAddress ip, int i){

        Range left = curNode.lowerKey(l);
        if(left!=null && left.end>=ip.getVal(0,i))
        {
            TreeMap<Range,TreeMap> temp = curNode.remove(left);
            TreeMap<Range,TreeMap> copy = new TreeMap<>();
            deepCopy(temp,copy);
            curNode.put(new Range(left.start,ip.getVal(0,i)-1),temp);
            curNode.put(new Range(ip.getVal(0,i),left.end),copy);
        }
        Range right = curNode.floorKey(r);

        if(right!=null && right.end>ip.getVal(1,i)){
            TreeMap<Range,TreeMap> temp = curNode.remove(right);
            TreeMap<Range,TreeMap> copy = new TreeMap<>();
            deepCopy(temp,copy);
            curNode.put(new Range(right.start,ip.getVal(1,i)),temp);
            curNode.put(new Range(ip.getVal(1,i)+1,right.end),copy);
        }

    }

    //When a range is split into two, we need to make a copy for the newly created range
    private void deepCopy(TreeMap<Range,TreeMap> from, TreeMap<Range,TreeMap> to){
        for(Range cur : from.keySet()){
            to.put(new Range(cur.start,cur.end),new TreeMap());
            deepCopy(from.get(cur),to.get(cur));
        }
    }

    //pre fills missing sub ranges
    private void fillRange(IPAddress ip, Map<Range,TreeMap> subMap, int i){
        int start = ip.getVal(0,i);
        List<Range> addLater = new LinkedList<>();
        for(Range curRange : subMap.keySet())
        {
            if(start<curRange.start)
                addLater.add(new Range(start,curRange.start - 1));
            start = curRange.end+1;
        }
        if(start<=ip.getVal(1,i))
            addLater.add(new Range(start,ip.getVal(1,i)));

        for(Range r : addLater)
            subMap.put(r,new TreeMap());
    }

    //To build tree, where each nodes contains valid range of certain parameter
    private void buildTree(IPAddress ip, int i, TreeMap<Range,TreeMap> curNode,boolean isRoot){
        if(i==limit)
            return;
        Range l = new Range(ip.getVal(0,i),ip.getVal(0,i));
        Range r = new Range(ip.getVal(1,i),ip.getVal(1,i));

        splitEdges(l, r, curNode, ip, i);
        Map<Range,TreeMap> temp = curNode.subMap(l,new Range(ip.getVal(1,i)+1,ip.getVal(1,i)+1));
        fillRange(ip,temp,i);

        addNewRules(ip,curNode,temp,i);

    }


    //To validate the packet
    protected boolean acceptPacket(String direction, String protocol, int port, String IpAddress){
        boolean isInBound = direction.equals("inbound");
        boolean isTcp = protocol.equals("tcp");
        TreeMap<Range,TreeMap> node = getNode(isInBound,isTcp);
        Integer[] parameters = getParameters(port,IpAddress);
        return findPath(node,0,parameters);
    }

    //to extact all the parameters of port and ipaddress
    private Integer[] getParameters(Integer port, String IpAddress){
        Integer[] parameters = new Integer[5];
        parameters[0] = port;

        int i=1;
        for(String t : IpAddress.split("\\.")) {
            parameters[i++] = Integer.parseInt(t);
        }
        return parameters;
    }

    //This function checks whether each parameters are valid or not one by one
    private boolean findPath(TreeMap<Range,TreeMap> cur, int i, Integer[] parameters){
        if(i==limit)
            return true;

        Range curRange = cur.floorKey(new Range(parameters[i],parameters[i]));
        if(curRange==null || curRange.end<parameters[i])
            return false;

        return findPath(cur.get(curRange),i+1,parameters);
    }

    //Object to hold the range of parameters
    class Range implements Comparable<Range>{
        int start,end;
        Range(int start,int end){
            this.start = start;
            this.end = end;
        }
        public int compareTo(Range r2){
            return Integer.compare(this.start,r2.start);
        }
    }

    private TreeMap<Range,TreeMap> getNode(boolean isInBound, boolean isTcp){
        // 0 - for inbound & tcp, 1 - for inbound & udp
        // 2 - for outbount & tcp, 3 - for inbound & udp
        if(isInBound)
            return isTcp ? nodes.get(0) : nodes.get(1);
        else
            return isTcp ? nodes.get(2) : nodes.get(3);
    }

}
