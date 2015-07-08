package com.xxx.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller 
@RequestMapping("/uploader") 
public class FileUpoadController {  
	
    @RequestMapping("/fileUpload.action")  
    public void handleUpload( HttpServletRequest request,HttpServletResponse response, 
        @RequestParam("file") MultipartFile file) {  
        //MultipartFile是对当前上传的文件的封装，当要同时上传多个文件时，可以给定多个MultipartFile参数  
    	PrintWriter out = null;
    	response.setContentType("application/json");
    	try {
 			out = response.getWriter();
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
    	if (!file.isEmpty()) {  
            try {
				/*System.out.println("文件长度: " + file.getSize()); 
                System.out.println("文件类型: " + file.getContentType()); 
                System.out.println("文件名称: " + file.getName()); 
                System.out.println("文件原名: " + file.getOriginalFilename());	*/
            	
                SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd/HH");
				String filePathDir = "/files"+dataFormat.format(new Date());
				String fileRealPathDir = request.getSession().getServletContext().getRealPath("/upload"+filePathDir);
				String chunk = (String)request.getParameter("chunk");  //分片序号
		    	if(null == chunk || chunk.equals("")){//没有分片
		    		FileUtils.copyInputStreamToFile(file.getInputStream(), new File(fileRealPathDir,file.getOriginalFilename()));
		    		out.write("{\"data\":\"success\"}");
		    		return ;
		    	}
		    	int chunks = Integer.parseInt((String)request.getParameter("chunks"));//分片总数   
		    	System.out.println("chunk: "+chunk+"|"+"文件长度: " + file.getSize());
                String tempfileName = file.getOriginalFilename()+ ".tmp_" + chunk;
                FileUtils.copyInputStreamToFile(file.getInputStream(), new File(fileRealPathDir,tempfileName));				
                boolean flag = true;              
                for(int i=0;i<chunks;i++){
                	tempfileName = file.getOriginalFilename() + ".tmp_" +i;
                	if(!FileUtils.directoryContains(new File(fileRealPathDir), new File(fileRealPathDir,tempfileName))){
                		flag = false;
                		break;
                	}
                }
                synchronized (this) {
	                if(flag && !FileUtils.directoryContains(new File(fileRealPathDir), new File(fileRealPathDir,file.getOriginalFilename()))){
	                	System.out.println("only one");
	                	File resultFile =  new File(fileRealPathDir,file.getOriginalFilename());
	                	FileOutputStream fos = new FileOutputStream(resultFile);
	                	FileInputStream fis = null;
	                	for(int i=0;i<chunks;i++){
	                		tempfileName = file.getOriginalFilename() + ".tmp_" +i;
	                		File tempfile = FileUtils.getFile(fileRealPathDir +"/"+tempfileName);
	                		fis = FileUtils.openInputStream(tempfile);	                		
	                		byte[] bytes = new byte[1024 * 1024];	                		
	                		int res ;
	                		while((res= fis.read(bytes))!=-1){
	                			fos.write(bytes, 0, res);  //当最后一次读 不足1024*1024时，将bytes中res长度的字节读入，避免读入bytes长度的字节
	                		}
	                		fis.close();
	                		tempfile.delete();//删除临时文件
	                	}	                	
	                	fos.close();	                	
	                }
	                
            	}
                out.write("{\"data\":\"success\"}");
				//在这里就可以对file进行处理了，可以根据自己的需求把它存到数据库或者服务器的某个文件夹  
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
             
       } else {  
		   out.write("failed");
       }  
    }
    
    @RequestMapping("md5check.action")
    //只判断当前目录下是否有文件
    public void chunkMd5checked(HttpServletRequest request,HttpServletResponse response){
    	PrintWriter out = null;
    	response.setContentType("application/json");
    	try {
 			out = response.getWriter();
 		} catch (IOException e) {
 		
 			e.printStackTrace();
 		}
    	String chunks = (String)request.getParameter("chunks");
    	String chunk = (String)request.getParameter("chunk");  //分片序号
    	String fileName = (String)request.getParameter("fileName");  //文件名
    	String chunkMd5 = (String)request.getParameter("chunkMd5");  //分片的md5值
    	//System.out.println(fileName+":"+chunkMd5);
    	boolean flag = false;
    	SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd/HH");
    	String filePathDir = "/files"+dataFormat.format(new Date());
    	String fileRealPathDir = request.getSession().getServletContext().getRealPath("/upload"+filePathDir);    	
    	File file = null;
    	if(null == chunks || chunks.equals("") || (Integer.parseInt(chunk) == (Integer.parseInt(chunks)-1))){//没有分片
    		file = new File(fileRealPathDir+"/"+fileName);
    	}
    	else{
    		//System.out.println(chunks+"|"+chunk);
    		String tempFileName = fileName + ".tmp_" + chunk;
    		file = new File(fileRealPathDir+"/"+tempFileName);  		
    	}
    	if(!file.exists()){
			flag = false;
		}else{
			try {
				String localmd5File = DigestUtils.md5Hex(FileUtils.openInputStream(file));
				//System.out.println(localmd5File);
				flag = localmd5File.equals(chunkMd5);
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
    	
    	out.write("{\"exists\":"+flag+"}");
    	out.close();
    }
    
    @RequestMapping("fileMd5check.action")
    public void fileMd5check(HttpServletRequest request,HttpServletResponse response){
    	PrintWriter out = null;
    	response.setContentType("application/json");
    	try {
 			out = response.getWriter();
 		} catch (IOException e) {
 			
 			e.printStackTrace();
 		}
    	String fileName = (String)request.getParameter("fileName");  //文件名
    	String fileMd5 = (String)request.getParameter("fileMd5");  //文件的md5值
    	SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd/HH");
    	String filePathDir = "/files"+dataFormat.format(new Date());
    	String fileRealPathDir = request.getSession().getServletContext().getRealPath("/upload"+filePathDir);    	
    	System.out.println(fileName + "|" +fileMd5);	
    	File file = new File(fileRealPathDir+"/"+fileName);
    	boolean flag = false;
    	boolean md5Equal = false;
    	if(!file.exists()){
    		flag = false;
    	}else{//文件名存在
			flag = true;
    		try {
				String localmd5File = DigestUtils.md5Hex(FileUtils.openInputStream(file));
				System.out.println(localmd5File);
				md5Equal = localmd5File.equals(fileMd5);
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
    	
    	
    	out.write("{\"exists\":"+flag+",\"md5Equal\":"+md5Equal+"}");
    	out.close();
    }
	  
  
}