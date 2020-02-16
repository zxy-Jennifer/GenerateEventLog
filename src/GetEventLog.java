import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
public class GetEventLog {
    private static HashMap<String ,List<String> > mappos = new HashMap<>();//存放正向map
    private static HashMap<String ,List<String> > mapneg = new HashMap<>();//存放反向map
    private static List<String> idList=new ArrayList<>();//存放变迁的id
    private static List<String> nameList=new ArrayList<>();//存放变迁的name
    private static List<Integer> countList=new ArrayList<>();//记录变迁访问的次数

    private static Set<String> res=new TreeSet<>();//存放日志信息

    private static String p_sou;//模型的源点
    private static String p_des;//模型的终点

    public static void  main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        getLogOfModel("E:\\研一上学期\\高级算法\\大作业\\附件1\\Model2.pnml",
                "E:\\研一上学期\\高级算法\\logOfModel2.txt");
    }

    private static void getLogOfModel(String modelFile,String logFile) throws IOException, ParserConfigurationException, SAXException {
        parserPnml(modelFile);
        List<String> places=new ArrayList<>();
        places.add(p_sou);
        ActivityTracer(places,"");
        System.out.println("res:"+res.size());
        Iterator it = res.iterator();
        int num=1;
        while (it.hasNext()) {
            String p=it.next().toString().trim();
            String newp="";
            String []pas=p.split(" ");
            //根据transition的id找到name并写入日志文件中
            for(String pa:pas){
                int index=idList.indexOf(pa);
                newp+=" "+nameList.get(index);
            }
            String con="case"+num+": "+newp+System.getProperty("line.separator");
            writeFile(con,logFile);
//            System.out.println(con);
            num++;
        }
    }
    private static boolean isEnd(String tra){//判断是否为并行结构的终点

        return mapneg.get(tra).size()>1;
    }
    private static boolean isOk(List<String> places,String traId){//判断tra的前继库所是否都已经访问过
        for(String place:places){
            if(!mappos.get(place).contains(traId))
                return false;
        }
        return true;
    }
    private static void addCount(String traId){
        int index=idList.indexOf(traId);
        int count=countList.get(index);
        countList.set(index,++count);
    }
    private static void redCount(String traId){
        int index=idList.indexOf(traId);
        int count=countList.get(index);
        countList.set(index,--count);
    }
    private static void ActivityTracer(List<String> places, String path){
        for(String place:places){
            if(place.equals(p_des)){
                res.add(path);
                return ;
            }
        }
        for(String place:places) {
            if (mappos.containsKey(place)) {//不是结束库所
                List<String> list = new ArrayList<>(mappos.get(place));//place对应的多个transition都加入到list中
                boolean maybeLoop=false;
                if(list.size()>=2)//可能存在环
                    maybeLoop=true;
                for (String tra : list) {//依次遍历多个transition对应的place
                    int index=idList.indexOf(tra);
                    List<String> nextPla = new ArrayList<>(mappos.get(tra));//out(tra)
                    String newPath = path + " " + idList.get(index);
                    List<String> newPlaces = new ArrayList<>(places);//

                    addCount(tra);
                    if(maybeLoop&&countList.get(index)>=2){//在可能是环的结构中限制transition的访问次数
                        redCount(tra);
                        continue;
                    }
                    //transition为并行结构的终点并且transition的前继都已经遍历
                    if (isEnd(tra) && isOk(places, tra)) {
                        ActivityTracer(nextPla, newPath);
                        //transition的前继还没有遍历完，继续循环开始下一轮的遍历
                    } else if (isEnd(tra)) {
                        break;
                    } else {
                        //transition不是并行结构的终点，
                        newPlaces.remove(place);
                        newPlaces.addAll(nextPla);
                        ActivityTracer(newPlaces, newPath);
                    }
                    redCount(tra);
                }
            }
        }
    }
    private static void parserPnml(String modelFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder=dbf.newDocumentBuilder();
        Document doc=documentBuilder.parse(new File(modelFile));

        // 得到变迁列表将每个变迁的id放入list中
        NodeList tranNodes=doc.getElementsByTagName("transition");
        for(int i=0;i<tranNodes.getLength();i++){
            Node tranNode=tranNodes.item(i);
            String tranId=tranNode.getAttributes().getNamedItem("id").getNodeValue();
            idList.add(tranId);
            countList.add(0);
            //变迁子节点
            NodeList tranChildNode=tranNode.getChildNodes();
            for(int j=0;j<tranChildNode.getLength();j++){//
                Node childNode=tranChildNode.item(j);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    for (Node node = childNode.getFirstChild(); node != null; node = node.getNextSibling()) {
                        if (node.getNodeName().equals("text")) {
                            Node titleNode = node.getFirstChild();
                            nameList.add(titleNode.getNodeValue());
                        }
                    }
                }
            }
        }
        //得到弧的列表并放到正向反向map中
        NodeList arcNodeList = doc.getElementsByTagName("arc");
        for (int i = 0; i < arcNodeList.getLength(); i++) {
            Node arcNode = arcNodeList.item(i);
            String source = arcNode.getAttributes().getNamedItem("source").getNodeValue();
            String target = arcNode.getAttributes().getNamedItem("target").getNodeValue();
            //正向map
            List<String> la=new ArrayList<>();
            if(mappos.containsKey(source))
                la.addAll(mappos.get(source));
            la.add(target);
            mappos.put(source,la );
            //反向map
            List<String> lb=new ArrayList<>();
            lb.add(source);
            if(mapneg.containsKey(target))
                lb.addAll(mapneg.get(target));
            mapneg.put(target,lb);
        }
        //遍历place，找到源点 和终点
        NodeList placeNodeList = doc.getElementsByTagName("place");
        for (int i = 0; i < placeNodeList.getLength(); i++) {
            Node placeNode = placeNodeList.item(i);
            String placeId=placeNode.getAttributes().getNamedItem("id").getNodeValue();
            if(!mapneg.containsKey(placeId))
                p_sou=placeId;
            if(!mappos.containsKey(placeId) )
                p_des=placeId;
        }
    }
    private static void writeFile(String context,String logFile) throws IOException {
        File file=new File(logFile);
        FileOutputStream fileOutputStream=new FileOutputStream(file,true);
        fileOutputStream.write(context.getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }
}

