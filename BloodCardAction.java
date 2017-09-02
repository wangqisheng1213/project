package com.jinglin.controller;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.jinglin.entity.AlreadyCheck;
import com.jinglin.entity.BloodCard;
import com.jinglin.entity.NotCheck;
import com.jinglin.service.AlreadyCheckService;
import com.jinglin.service.BloodCardService;
import com.jinglin.service.NotCheckService;
import com.jinglin.util.ReadExcelUtil;

@Controller
@RequestMapping("bloodCard")
public class BloodCardAction {
	@Autowired
	private BloodCardService bloodCardService;
	
	@Autowired
	private AlreadyCheckService alreadyCheckService;//不需检验(已检)
	
	@Autowired
	private NotCheckService notCheckService;//未检验 

	/**
	 * 检查是否有重复的数据
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * */
	 @SuppressWarnings("unused")
    @ResponseBody
    @RequestMapping(params= "checkblood")
	public Map<String,Object> checkBlood(HttpServletRequest request,HttpServletResponse response) throws FileNotFoundException, IOException{
		 SimpleDateFormat sdf =   new SimpleDateFormat( " yyyy-MM-dd HH:mm:ss " );
		 
		String bloodCard = request.getParameter("bloodCard").trim();
		List<BloodCard> bloodList =  bloodCardService.findBy("bloodNo", bloodCard);
		if(bloodList.size()>0){//重复数据执行的方法
			System.out.println("总表中查到的血卡编号："+bloodList.get(0).getBloodNo());
			AlreadyCheck alreadyCheck = new AlreadyCheck();
			Map<String, Object> map = new HashMap<String, Object>();
			for(int i = 0 ;i<bloodList.size();i++){
				alreadyCheck.setCheckTime(new Date());
				alreadyCheck.setBloodNo(bloodList.get(i).getBloodNo());
				alreadyCheck.setIDcard(bloodList.get(i).getIDcard());
				List<AlreadyCheck> lists = alreadyCheckService.findBy("bloodNo", bloodCard);
				String time = "";
				if(lists.size()>0){
					time = sdf.format(lists.get(0).getCheckTime());
				}else{
					alreadyCheckService.saveOrUpdate(alreadyCheck);
				}
				map.put("time", time);
				}
			//按id排序 降序  取前92条
			Long count = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
			List<AlreadyCheck> alreadyCountList = null;
			if(count>92){
				alreadyCountList = alreadyCheckService.getListForHQL("from AlreadyCheck order by id desc",92);
			}else{
				alreadyCountList = alreadyCheckService.getAll("id", false);
			}
			if(count%92==0){
				writeExcelAlready(alreadyCountList);
				map.put("SAOMA", "YES");
			}
			 map.put("idone", alreadyCountList.get(0).getId());
			 map.put("timeone", sdf.format(alreadyCountList.get(0).getCheckTime()));
			 int idtwo = 0;
			 String bloodtwo = "";
			 String timetwo = "";
			 if(count>1){
				 idtwo = alreadyCountList.get(1).getId();
				 bloodtwo =  alreadyCountList.get(1).getBloodNo();
				 timetwo = sdf.format(alreadyCountList.get(1).getCheckTime());
			 }
			 map.put("bloodone", alreadyCountList.get(0).getBloodNo());
			 map.put("idtwo", idtwo);
			 map.put("bloodtwo", bloodtwo);
			 map.put("timetwo", timetwo);
			 map.put("alreadyCount", count);//总重复数据的条数
			 Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");//总正常数据的条数
			 map.put("notCheckCount", notCheckCount);
			 map.put("status", "YES");
			return map;
		}else{//正常数据
			NotCheck notCheck  = new NotCheck();
			notCheck.setBloodNo(bloodCard);
			notCheck.setCheckTime(new Date());
			notCheck.setStatus("0");//读数据到数据库，未写入到Excel
			 Map<String, Object> map = new HashMap<String, Object>();
			List<NotCheck> list = notCheckService.findBy("bloodNo", bloodCard);//查询此血卡是否已经扫描过
			String time = "";
			if(list.size()>0){
				time = sdf.format(list.get(0).getCheckTime());
			}else{
				notCheckService.saveOrUpdate(notCheck);
			}
			map.put("time", time);
			Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");
			List<NotCheck> notChecks = null;
			if(notCheckCount>92){
				notChecks = notCheckService.getListForHQL("from NotCheck order by id desc",92);//查询出未写入Excel的数量(desc)
			}else{
				notChecks = notCheckService.getAll("id", false);
			}
			if(notCheckCount%92==0){
				writeExcel(notChecks);
				map.put("SAOMA", "YES");
			}
			 map.put("idone", notChecks.get(0).getId());
			 map.put("bloodone", notChecks.get(0).getBloodNo());
			 map.put("timeone", sdf.format(notChecks.get(0).getCheckTime()));
			 int idtwo = 0;
			 String bloodtwo = "";
			 String timetwo = "";
			 if(notChecks.size()>1){
				 idtwo = notChecks.get(1).getId();
				 bloodtwo =  notChecks.get(1).getBloodNo();
				 timetwo = sdf.format(notChecks.get(1).getCheckTime());
			 }
			 map.put("idtwo", idtwo);
			 map.put("timetwo", timetwo);
			 map.put("bloodtwo",bloodtwo );
			 Long alreadyCount = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
			 map.put("alreadyCount", alreadyCount);
			 map.put("notCheckCount", notCheckCount);
			 map.put("status", "NO");
			return map;
		}
		
	}
	 /**
	  * 正常数据产生Excel
	  * */
    public void writeExcel(List<NotCheck> notChecks) throws IOException{
    	String file = "C:/GZFA0000.xls";
    	
    	ReadExcelUtil.preReadCheck(file);
    	Workbook workbook = ReadExcelUtil.getWorkbook(file);
    	
    	// 循环工作表Sheet
         for (int numSheet = 0; numSheet < 1; numSheet++) {
	           Sheet copySheet1 = workbook.getSheetAt(1);
	           if (copySheet1 == null) {
	                 continue;
	           }

        	 Sheet hssfSheet = workbook.getSheet("录入");
	           if (hssfSheet == null) {
	                 continue;
	            }
	           	int first = (int) (Math.random()*10+1);
	       		int second = (int) (Math.random()*10+1);
	       		Map<String, Integer> map = recursion(first, second);
	       		int one = map.get("first");
	       		int two = map.get("second");
	       		int firstF = (int) (Math.random()*6+1);
	       		int secondF = (int) (Math.random()*6+1);
	       		Map<String, Integer> mapS = recursionForSeven(firstF, secondF);
	       		int oneS = mapS.get("first");
	       		int twoS = mapS.get("second");
	       		String ninenine = (char)(int)(oneS+'A')+String.format("%02d", one);
	       		String water = (char)(int)((oneS+1)+'A')+String.format("%02d", one+1);
	       		String ladder1 = (char)(int)(twoS+'A')+String.format("%02d", two);
	       		String ladder2 = (char)(int)((twoS+1)+'A')+String.format("%02d", two+1);
	           for (int rowNum = 1; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
	        	   				Row hssfRow = hssfSheet.getRow(rowNum);
	        	                    if (hssfRow != null) {
	        	                		Row row = hssfSheet.getRow(rowNum);
	        	                		if(row!=null){
	        	                			String cellRow = row.getCell(0).toString();
	        	                			if (cellRow.equals(ninenine)) {
	        	                				row.getCell(1).setCellValue("9948");
	        	        					}
	        	                			if (cellRow.equals(water)) {
	        	                				row.getCell(1).setCellValue("water");
	        	        					}
	        	                			if (cellRow.equals(ladder1)) {
	        	                				row.getCell(1).setCellValue("ladder");
	        	        					}
	        	                			if (cellRow.equals(ladder2)) {
	        	                				row.getCell(1).setCellValue("ladder");
	        	        					}
	        	                		}
	        	                    }
	        	                }
        	int temp = 0;
        	int rowCount = 4;
        	int cellCount = 1;
        	int tempone = 0;
        	int temptwo = 0;
        	for(int m = 1;m<=96;m++){
        			Cell cell = hssfSheet.getRow(m).getCell(1);
        			Row row = hssfSheet.getRow(m);
        			String string = row.getCell(0).toString();
        			Cell copyCell = copySheet1.getRow(rowCount+tempone).getCell(cellCount+temptwo);
        			if (string.equals(ninenine)||string.equals(water)||string.equals(ladder1)||string.equals(ladder2)) {
        				if(string.equals(ninenine)){
        					cell.setCellValue("9948");
        					copyCell.setCellValue("9948");
        				}
        				if(string.equals(water)){
        					cell.setCellValue("water");
        					copyCell.setCellValue("water");
        				}
        				if(string.equals(ladder1)){
        					cell.setCellValue("ladder");
        					copyCell.setCellValue("ladder");
        				}
        				if(string.equals(ladder2)){
        					cell.setCellValue("ladder");
        					copyCell.setCellValue("ladder");
        				}
					}else {
						cell.setCellValue(notChecks.get(temp).getBloodNo());
						copyCell.setCellValue(notChecks.get(temp).getBloodNo());
						temp ++;
					}
        			tempone++;
        			if(m%8==0){
        				tempone = 0;
        				temptwo = m/8;
        			}
        	}
    	}
         FileOutputStream out = new FileOutputStream("C:/Users/GoldenEye_SM/Desktop/正常数据/"+System.currentTimeMillis()+".xls");
         out.flush();
         workbook.write(out);
         out.flush();
         out.close();
     }
    /**
     * 重复数据产生Excel
     * */
    public void writeExcelAlready(List<AlreadyCheck> alreadyCheck) throws IOException{
    	String file = "C:/GZFA0000.xls";
    	
    	ReadExcelUtil.preReadCheck(file);
    	Workbook workbook = ReadExcelUtil.getWorkbook(file);
    	
    	// 循环工作表Sheet
         for (int numSheet = 0; numSheet < workbook.getNumberOfSheets(); numSheet++) {
        	  Sheet copySheet1 = workbook.getSheetAt(1);
	           if (copySheet1 == null) {
	                 continue;
	           }
        	 Sheet hssfSheet = workbook.getSheet("录入");
	           if (hssfSheet == null) {
	                 continue;
	            }
	           	int first = (int) (Math.random()*10+1);
	       		int second = (int) (Math.random()*10+1);
	       		Map<String, Integer> map = recursion(first, second);
	       		int one = map.get("first");
	       		int two = map.get("second");
	       		int firstF = (int) (Math.random()*6+1);
	       		int secondF = (int) (Math.random()*6+1);
	       		Map<String, Integer> mapS = recursionForSeven(firstF, secondF);
	       		int oneS = mapS.get("first");
	       		int twoS = mapS.get("second");
	       		String ninenine = (char)(int)(oneS+'A')+String.format("%02d", one);
	       		String water = (char)(int)((oneS+1)+'A')+String.format("%02d", one+1);
	       		String ladder1 = (char)(int)(twoS+'A')+String.format("%02d", two);
	       		String ladder2 = (char)(int)((twoS+1)+'A')+String.format("%02d", two+1);
	           for (int rowNum = 1; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
	        	   				Row hssfRow = hssfSheet.getRow(rowNum);
	        	                    if (hssfRow != null) {
	        	                		Row row = hssfSheet.getRow(rowNum);
	        	                		if(row!=null){
	        	                			String cellRow = row.getCell(0).toString();
	        	                			if (cellRow.equals(ninenine)) {
	        	                				row.getCell(1).setCellValue("9948");
	        	        					}
	        	                			if (cellRow.equals(water)) {
	        	                				row.getCell(1).setCellValue("water");
	        	        					}
	        	                			if (cellRow.equals(ladder1)) {
	        	                				row.getCell(1).setCellValue("ladder");
	        	        					}
	        	                			if (cellRow.equals(ladder2)) {
	        	                				row.getCell(1).setCellValue("ladder");
	        	        					}
	        	                		}
	        	                    }
	        	                }
        	int temp = 0;
        	int rowCount = 4;
        	int cellCount = 1;
        	int tempone = 0;
        	int temptwo = 0;
        	for(int m = 1;m<=96;m++){
        			Cell cell = hssfSheet.getRow(m).getCell(1);
        			Row row = hssfSheet.getRow(m);
        			String string = row.getCell(0).toString();
        			Cell copyCell = copySheet1.getRow(rowCount+tempone).getCell(cellCount+temptwo);
        			if (string.equals(ninenine)||string.equals(water)||string.equals(ladder1)||string.equals(ladder2)) {
        				if(string.equals(ninenine)){
        					cell.setCellValue("9948");
        					copyCell.setCellValue("9948");
        				}
        				if(string.equals(water)){
        					cell.setCellValue("water");
        					copyCell.setCellValue("water");
        				}
        				if(string.equals(ladder1)){
        					cell.setCellValue("ladder");
        					copyCell.setCellValue("ladder");
        				}
        				if(string.equals(ladder2)){
        					cell.setCellValue("ladder");
        					copyCell.setCellValue("ladder");
        				}
					}else {
						cell.setCellValue(alreadyCheck.get(temp).getBloodNo());
						copyCell.setCellValue(alreadyCheck.get(temp).getBloodNo());
						temp ++;
					}
        			tempone++;
        			if(m%8==0){
        				tempone = 0;
        				temptwo = m/8;
        			}
        	}
    	}//C:/Users/Administrator/Desktop/already/
         FileOutputStream out = new FileOutputStream("C:/Users/GoldenEye_SM/Desktop/重复数据/"+System.currentTimeMillis()+".xls");
         workbook.write(out);
         out.close();
      }
	
    
    
    /**
     * 导入数据到数据库
     * @throws IOException 
     * */
    public void insertDataForDbaForXLSX(InputStream io) throws IOException{
		XSSFWorkbook hssfWorkbook = new XSSFWorkbook(io);
    	// 循环工作表Sheet
         for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
        	 XSSFSheet xssfSheet = hssfWorkbook.getSheetAt(numSheet);
	           if (xssfSheet == null) {
	                 continue;
	            }
	           for (int rowNum = 1; rowNum <= xssfSheet.getLastRowNum(); rowNum++) {
	        	   XSSFRow xssfRow = xssfSheet.getRow(rowNum);
	        	   BloodCard bloodCardEnt = new BloodCard();
	        	   if (xssfRow != null) {
	        		   bloodCardEnt.setInsertTime(new Date());
	        		   bloodCardEnt.setBloodNo(xssfRow.getCell(0).toString().trim()==null?"":xssfRow.getCell(0).toString().trim());
	        		   bloodCardEnt.setIDcard(xssfRow.getCell(1).toString().trim()==null?"":xssfRow.getCell(1).toString().trim());
	        	   }
	        	   int count = bloodCardService.findBy("bloodNo", bloodCardEnt.getBloodNo()).size();
	        	   if(count<1){
	        		   bloodCardService.saveOrUpdate(bloodCardEnt);
	        	   }
	        	}
         }
    }
 public void insertDataForDbaForXLS(InputStream io) throws IOException{
		HSSFWorkbook hssfWorkbook = new HSSFWorkbook(io);
    	// 循环工作表Sheet
         for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
        	 HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
	           if (hssfSheet == null) {
	                 continue;
	            }
	           for (int rowNum = 1; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
	        	   HSSFRow hssfRow = hssfSheet.getRow(rowNum);
	        	   BloodCard bloodCardEnt = new BloodCard();
	        	   if (hssfRow != null) {
	        		   bloodCardEnt.setInsertTime(new Date());
	        		   bloodCardEnt.setBloodNo(hssfRow.getCell(0).toString().trim()==null?"":hssfRow.getCell(0).toString().trim());
	        		   bloodCardEnt.setIDcard(hssfRow.getCell(1).toString().trim()==null?"":hssfRow.getCell(1).toString().trim());
	        	   }
	        	   int count = bloodCardService.findBy("bloodNo", bloodCardEnt.getBloodNo()).size();
	        	   if(count<1){
	        		   bloodCardService.saveOrUpdate(bloodCardEnt);
	        	   }
	        	}
         }
    }
    
    @SuppressWarnings("unused")
   	@RequestMapping(params = "insertUpload")
       @ResponseBody
   	public String insertUpload(@RequestParam("file") CommonsMultipartFile files[]) {
    	final String xls = "xls";  
        final String xlsx = "xlsx";  
       	Map<String,Object> map = new HashMap<String,Object>();
       	if(files==null||files.length<1){
       		//map.put("success", false);
       		return "false";
       	}
       	String fileName = files[0].getOriginalFilename();
       	System.out.println(fileName);
       	try {
       		InputStream is = files[0].getInputStream();
       		if(fileName.endsWith(xls)){
       			insertDataForDbaForXLS(is);
       		}
       		if(fileName.endsWith(xlsx)){
       			
       			insertDataForDbaForXLSX(is);
       		}
       		return "true";
   		} catch (IOException e) {
   			e.printStackTrace();
   			return "false";
   		}
   	}
    
    /**
     * 修改数据
     * @throws ParseException 
     * */
    @RequestMapping(params = "updateBlood")
       @ResponseBody
   	public Map<String, Object> updateBlood(HttpServletRequest request,HttpServletResponse response) throws ParseException {
    	SimpleDateFormat sdf =   new SimpleDateFormat( " yyyy-MM-dd HH:mm:ss " );
    	Map<String, Object> map = new HashMap<String, Object>();
    	String ststus = request.getParameter("ststus");
    	String id = request.getParameter("id");
    	String blood = request.getParameter("blood");
    	String time = request.getParameter("time");
    	int count = bloodCardService.findBy("bloodNo", blood).size();
    	int countAlready = alreadyCheckService.findBy("bloodNo", blood).size();
    	int notCount = notCheckService.findBy("bloodNo", blood).size();
    	String idcard ="";
    	if(count>0){
    		idcard = bloodCardService.findBy("bloodNo", blood).get(0).getIDcard();
    	}
    	
    	if(count>0){
    		if(ststus.equals("YES")&&countAlready<1){
        		AlreadyCheck already = new AlreadyCheck();
        		already.setBloodNo(blood);
        		already.setId(Integer.parseInt(id));
        		already.setCheckTime(sdf.parse(time));
        		already.setIDcard(idcard);
        		alreadyCheckService.saveOrUpdate(already);
        		Long alreadyCount = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
        		List<AlreadyCheck> alreadyCountList = null;
        		if(alreadyCount>92){
        			alreadyCountList = alreadyCheckService.getListForHQL("from AlreadyCheck order by id desc",92);
        		}else{
        			alreadyCountList = alreadyCheckService.getAll("id", false);
        		}
    			 map.put("idone", alreadyCountList.get(0).getId());
    			 map.put("bloodone", alreadyCountList.get(0).getBloodNo());
    			 map.put("timeone", sdf.format(alreadyCountList.get(0).getCheckTime()));
    			 int idtwo = 0;
    			 String bloodtwo = "";
    			 String timetwo = "";
    			 if(alreadyCountList.size()>2){
    				 idtwo = alreadyCountList.get(1).getId();
    				 bloodtwo = alreadyCountList.get(1).getBloodNo();
    				 timetwo = sdf.format(alreadyCountList.get(1).getCheckTime());
    			 }
    			 map.put("timetwo", timetwo);
    			 map.put("idtwo", idtwo);
    			 map.put("bloodtwo", bloodtwo);
    			 map.put("alreadyCount", alreadyCount);
    			 Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");
    			 map.put("notCheckCount",notCheckCount);
    			 map.put("status", "YES");
        		map.put("SUCCESS", "SUCCESS");
        	}
    	}else if(ststus.equals("NO")&&notCount<1){
    		NotCheck not = new NotCheck();
    		not.setId(Integer.parseInt(id));
    		not.setBloodNo(blood);
    		not.setCheckTime(sdf.parse(time));
    		notCheckService.saveOrUpdate(not);
    		Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");
    		List<NotCheck> notChecks = null;
    		if(notCheckCount>92){
    			notChecks = notCheckService.getListForHQL("from NotCheck order by id desc",92);//查询出未写入Excel的数量(desc)
    		}else{
    			notChecks = notCheckService.getAll("id", false);
    		}
    		
			 map.put("idone", notChecks.get(0).getId());
			 map.put("bloodone", notChecks.get(0).getBloodNo());
			 map.put("timeone", sdf.format(notChecks.get(0).getCheckTime()));
			 int idtwo = 0;
			 String bloodtwo = "";
			 String timetwo = "";
			 if(notChecks.size()>1){
				 idtwo = notChecks.get(1).getId();
				 bloodtwo = notChecks.get(1).getBloodNo();
				 timetwo = sdf.format(notChecks.get(1).getCheckTime());
			 }
			 map.put("timetwo", timetwo);
			 map.put("idtwo", idtwo);
			 map.put("bloodtwo", bloodtwo);
			 Long alreadyCount = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
			 map.put("alreadyCount", alreadyCount);
			 map.put("notCheckCount",notCheckCount);
			 map.put("status", "NO");
   		     map.put("SUCCESS", "SUCCESS");
    	}else{
        		map.put("COUNT", "ONE");
        	}
    	return map;
    }
    /**
     * 删除数据
     * */
    @RequestMapping(params = "deleteBlood")
       @ResponseBody
   	public Map<String, Object> deleteBlood(HttpServletRequest request,HttpServletResponse response) {
    	SimpleDateFormat sdf =   new SimpleDateFormat( " yyyy-MM-dd HH:mm:ss " );
    	Map<String, Object> map = new HashMap<String, Object>();
    	String ststus = request.getParameter("ststus");
    	String id = request.getParameter("id");
    	String blood = request.getParameter("blood");
    	if(ststus.equals("NO")){
    		NotCheck not = new NotCheck();
    		not.setId(Integer.parseInt(id));
    		not.setBloodNo(blood);
    		notCheckService.remove(not);
    		Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");
    		List<NotCheck> notChecks = null;
    		if(notCheckCount>92){
    			notChecks = notCheckService.getListForHQL("from NotCheck order by id desc",92);//查询出未写入Excel的数量(desc)
    		}else{
    			notChecks = notCheckService.getAll("id", false);
    		}
			 map.put("idone", notChecks.get(0).getId());
			 map.put("bloodone", notChecks.get(0).getBloodNo());
			 map.put("timeone", sdf.format(notChecks.get(0).getCheckTime()));
			 int idtwo = 0;
			 String bloodtwo = "";
			 String timetwo = "";
			 if(notChecks.size()>2){
				 idtwo = notChecks.get(1).getId();
				 bloodtwo = notChecks.get(1).getBloodNo();
				 timetwo = sdf.format(notChecks.get(1).getCheckTime());
			 }
			 map.put("timetwo", timetwo);
			 map.put("idtwo", idtwo);
			 map.put("bloodtwo", bloodtwo);
			 Long alreadyCount = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
			 map.put("alreadyCount", alreadyCount);
			 map.put("notCheckCount",notCheckCount);
			 map.put("status", "NO");
  		     map.put("SUCCESS", "SUCCESS");
    	}
    	if(ststus.equals("YES")){
    		AlreadyCheck already = new AlreadyCheck();
    		already.setBloodNo(blood);
    		already.setId(Integer.parseInt(id));
    		alreadyCheckService.remove(already);
    		Long alreadyCount = (Long) alreadyCheckService.findcountbyHQL("select count(*) from AlreadyCheck");
    		List<AlreadyCheck> alreadyCountList =  null;
    		if(alreadyCount>92){
    			alreadyCountList = alreadyCheckService.getListForHQL("from AlreadyCheck order by id desc",92);
    		}else{
    			alreadyCountList = alreadyCheckService.getAll("id",false);
    		}
			 map.put("idone", alreadyCountList.get(0).getId());
			 map.put("bloodone", alreadyCountList.get(0).getBloodNo());
			 map.put("timeone", sdf.format(alreadyCountList.get(0).getCheckTime()));
			 int idtwo = 0;
			 String bloodtwo = "";
			 String timetwo = "";
			 if(alreadyCountList.size()>2){
				 idtwo = alreadyCountList.get(1).getId();
				 bloodtwo = alreadyCountList.get(1).getBloodNo();
				 timetwo = sdf.format(alreadyCountList.get(1).getCheckTime());
			 }
			 map.put("timetwo", timetwo);
			 map.put("idtwo", idtwo);
			 map.put("bloodtwo", bloodtwo);
			 map.put("alreadyCount", alreadyCount);
			 Long notCheckCount =  (Long)notCheckService.findcountbyHQL("select count(*) from NotCheck");
			 map.put("notCheckCount",notCheckCount);
			 map.put("status", "YES");
   		map.put("SUCCESS", "SUCCESS");
    	}
    	return map;
    }
    
    /*
     * 递归产生随机数
     * */
    public Map<String, Integer> recursion(int first,int second){
    	 Map<String, Integer> map = new HashMap<String, Integer>();
		if(first==second||first>second-2){
			first = (int) (Math.random()*11+1);
    		second = (int) (Math.random()*11+1);
    		return recursion(first, second);
		}
		map.put("first", first);
		map.put("second", second);
		return map;
    }
    
    public Map<String, Integer> recursionForSeven(int first,int second){
   	 Map<String, Integer> map = new HashMap<String, Integer>();
		if(first==second||first>second-2){
			first = (int) (Math.random()*6+1);
			second = (int) (Math.random()*6+1);
			return recursionForSeven(first, second);
		}
		map.put("first", first);
		map.put("second", second);
		return map;
   }
}
