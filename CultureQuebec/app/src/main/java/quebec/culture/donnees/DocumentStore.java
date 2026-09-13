package quebec.culture.donnees;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DocumentStore {
    public static final class Item {
        public String id,name,sector,fileName,mappingFile;
        public long createdAt;
        public int rows,columns,emptyCells,duplicates,changedCells,score;
    }
    private DocumentStore(){}
    private static File indexFile(Context c){return new File(c.getFilesDir(),"culture_documents.json");}
    public static List<Item> list(Context c){
        List<Item> out=new ArrayList<>();File f=indexFile(c);if(!f.exists())return out;
        try{JSONArray a=new JSONArray(new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Item x=new Item();x.id=o.optString("id");x.name=o.optString("name");x.sector=o.optString("sector");x.fileName=o.optString("fileName");x.mappingFile=o.optString("mappingFile");x.createdAt=o.optLong("createdAt");x.rows=o.optInt("rows");x.columns=o.optInt("columns");x.emptyCells=o.optInt("emptyCells");x.duplicates=o.optInt("duplicates");x.changedCells=o.optInt("changedCells");x.score=o.optInt("score",100);out.add(x);}}catch(Exception ignored){}
        out.sort((a,b)->Long.compare(b.createdAt,a.createdAt));return out;
    }
    public static Item save(Context c,String displayName,DataNormalizer.Sector sector,DataNormalizer.Report report)throws Exception{
        String id="doc_"+System.currentTimeMillis(),fn=id+".csv",mf=id+"-mapping.json";
        java.nio.file.Files.write(new File(c.getFilesDir(),fn).toPath(),SpreadsheetReader.toCsv(report.cleaned).getBytes(StandardCharsets.UTF_8));
        JSONArray maps=new JSONArray();for(DataNormalizer.ColumnMapping m:report.mappings){JSONObject o=new JSONObject();o.put("sourceHeader",m.sourceHeader);o.put("normalizedHeader",m.normalizedHeader);o.put("targetField",m.targetField);o.put("sampleBefore",m.sampleBefore);o.put("sampleAfter",m.sampleAfter);o.put("status",m.status);o.put("reason",m.reason);maps.put(o);}java.nio.file.Files.write(new File(c.getFilesDir(),mf).toPath(),maps.toString(2).getBytes(StandardCharsets.UTF_8));
        Item x=new Item();x.id=id;x.name=displayName;x.sector=sector.name();x.fileName=fn;x.mappingFile=mf;x.createdAt=System.currentTimeMillis();x.rows=report.rows;x.columns=report.columns;x.emptyCells=report.emptyCells;x.duplicates=report.duplicateRows;x.changedCells=report.changedCells;
        int denom=Math.max(1,report.rows*Math.max(1,report.columns));int penalty=Math.min(100,(int)Math.round((report.emptyCells*70.0+report.duplicateRows*30.0)/denom));x.score=Math.max(0,100-penalty);
        List<Item> all=list(c);all.add(0,x);writeIndex(c,all);return x;
    }
    private static void writeIndex(Context c,List<Item> all)throws Exception{JSONArray a=new JSONArray();for(Item x:all){JSONObject o=new JSONObject();o.put("id",x.id);o.put("name",x.name);o.put("sector",x.sector);o.put("fileName",x.fileName);o.put("mappingFile",x.mappingFile==null?"":x.mappingFile);o.put("createdAt",x.createdAt);o.put("rows",x.rows);o.put("columns",x.columns);o.put("emptyCells",x.emptyCells);o.put("duplicates",x.duplicates);o.put("changedCells",x.changedCells);o.put("score",x.score);a.put(o);}java.nio.file.Files.write(indexFile(c).toPath(),a.toString(2).getBytes(StandardCharsets.UTF_8));}
    public static Item find(Context c,String id){for(Item x:list(c))if(x.id.equals(id))return x;return null;}
    public static List<List<String>> read(Context c,Item x)throws Exception{try(InputStream in=new FileInputStream(new File(c.getFilesDir(),x.fileName))){return SpreadsheetReader.readCsv(in);}}
    public static List<DataNormalizer.ColumnMapping> readMappings(Context c,Item x)throws Exception{
        List<DataNormalizer.ColumnMapping> out=new ArrayList<>();if(x.mappingFile==null||x.mappingFile.isEmpty())return out;File f=new File(c.getFilesDir(),x.mappingFile);if(!f.exists())return out;
        JSONArray a=new JSONArray(new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);DataNormalizer.ColumnMapping m=new DataNormalizer.ColumnMapping();m.sourceHeader=o.optString("sourceHeader");m.normalizedHeader=o.optString("normalizedHeader");m.targetField=o.optString("targetField");m.sampleBefore=o.optString("sampleBefore");m.sampleAfter=o.optString("sampleAfter");m.status=o.optString("status");m.reason=o.optString("reason");out.add(m);}return out;
    }
}
