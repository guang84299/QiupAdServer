package com.guang.web.action;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Resource;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;












import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.guang.web.dao.QueryResult;
import com.guang.web.mode.GApp;
import com.guang.web.mode.GArea;
import com.guang.web.mode.GNetworkOperator;
import com.guang.web.mode.GPhoneModel;
import com.guang.web.mode.GUser;
import com.guang.web.mode.GUserStt;
import com.guang.web.service.GAppService;
import com.guang.web.service.GAreaService;
import com.guang.web.service.GNetworkOperatorService;
import com.guang.web.service.GPhoneModelService;
import com.guang.web.service.GUserService;
import com.guang.web.service.GUserSttService;
import com.guang.web.tools.GZipTool;
import com.guang.web.tools.StringTools;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;


public class GUserAction extends ActionSupport{

	private static final long serialVersionUID = -6570772391551890119L;
	private final static Logger logger = LoggerFactory.getLogger(GUserAction.class);
	@Resource private  GUserService userService;
	@Resource private GAppService appService;
	@Resource private GUserSttService userSttService;
	@Resource private GAreaService areaService;
	@Resource private GNetworkOperatorService gNetworkOperatorService;
	@Resource private GPhoneModelService gPhoneModelService;
	
	private File source;
	private String sourceFileName;
	
	public String list()
	{
		QueryResult<GUser>  qr = userService.findAlls(0);
		
		String sindex = ServletActionContext.getRequest().getParameter("index");
		int index = 0;
		if (sindex != null && !"".equals(sindex))
			index = Integer.parseInt(sindex);
		Long num = qr.getNum();
		int start = index * 100;
		if (start > num) {
			start = 0;
		}
		
		List<GUser> userList = userService.findAlls(start).getList();
		if(userList == null)
		{
			userList = new ArrayList<GUser>();
		}
		long time = System.currentTimeMillis();
		for(GUser u : userList)
		{
			if(u.getPhoneNumber() == null || "".equals(u.getPhoneNumber()))
				u.setPhoneNumber("未知");
			if(time - u.getUpdatedDate().getTime() > 3*60*60*1000)
			{
				u.setOnline(false);
			}
			else
			{
				u.setOnline(true);
			}
		}
		
		ActionContext.getContext().put("maxNum", num);
		ActionContext.getContext().put("userList", userList);
		ActionContext.getContext().put("pages", "user");
		
		return "index";
	}
	
	//
	public void uploadAppInfos()
	{
		String data = ServletActionContext.getRequest().getParameter("data");
		JSONObject obj = JSONObject.fromObject(data);
		
		String name = obj.getString("name");
		String password = obj.getString("password");
		String packageName = obj.getString("packageName");
		String versionName = null;
		String sdkVersion = null;	
		
		GUser  user = userService.find(obj.getString("id"),password);
		
		if(obj.containsKey("versionName"))
			versionName = obj.getString("versionName");
		if(obj.containsKey("sdkVersion"))
			sdkVersion = obj.getString("sdkVersion");
		
		if(StringTools.isEmpty(versionName))
			versionName = "1.0";
		if(StringTools.isEmpty(sdkVersion))
			sdkVersion = "1.0";
		
		boolean isExist = false;
		List<GApp> list = appService.findAppsByUserId(user.getId()).getList();
		for(GApp a : list)
		{
			if(a.getPackageName().equals(packageName))
			{
				isExist = true;
				a.setUpdateSdkVersion(sdkVersion);
				a.setUpdateVersionName(versionName);
				appService.update(a);
				break;
			}
		}
		if(!isExist)
		{
			GApp app = new GApp(user.getId(), name, packageName,versionName,sdkVersion);
			appService.add(app);
		}
		
		try {
			ServletActionContext.getResponse().getWriter().print(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//删除user
	public void deleteUser()
	{
		String id = ServletActionContext.getRequest().getParameter("data");
		if(id != null && !"".equals(id))
		{
			long ids = Long.parseLong(id);
			userService.delete(ids);
			
			List<GApp> list = appService.findAppsByUserId(ids).getList();
			for(GApp app : list)
			{
				appService.delete(app.getId());
			}
		}
	}
	
	//初始化数据
	public void init()
	{
//		GSysVal sysVal = new GSysVal(0, false, 2, "", "", 0, 1.0f);
//		sysValService.save(sysVal);
		
		userSttService.add(new GUserStt(0l,0l,0l, 0l, 0l, 0l));
	}

	//更新app model
	public void updateAppModel()
	{
		List<GApp> list = appService.findApps(0, 100000000).getList();
		for(GApp app : list)
		{
			if(StringTools.isEmpty(app.getVersionName()) || StringTools.isEmpty(app.getSdkVersion()))
			{
				app.setVersionName("1.0");
				app.setSdkVersion("1.0");
				appService.update(app);
			}
		}
	}
	public void print(Object data) {
		try {
			ServletActionContext.getResponse().getWriter().print(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 查询用户
	 */
	public String findUser(){
		String regFrom = ServletActionContext.getRequest().getParameter("regDate_from");
		String loginFrom = ServletActionContext.getRequest().getParameter("loginDate_from");
		String regTo = ServletActionContext.getRequest().getParameter("regDate_to");
		String loginTo = ServletActionContext.getRequest().getParameter("loginDate_to");
		
		List<GUser> gUser = null ; 
		//注册时间
		if (null!=regFrom&&!"".equals(regFrom) && null!=regTo&&!"".equals(regTo)) {
			LinkedHashMap<String, String> colvals = new LinkedHashMap<String, String>();			
			colvals.put("createdDate >=", "'"+regFrom+"'");
			colvals.put("createdDate <", "'"+regTo+"'");
			gUser = userService.find(colvals).getList();
			long m = userService.find(colvals).getNum();
			ActionContext.getContext().put("maxNum", m);
			ActionContext.getContext().put("userList", gUser);
			ActionContext.getContext().put("pages", "user");
			return "index";	 
		}
		if (null!=loginFrom&&!"".equals(loginFrom) && null!=loginTo&&!"".equals(loginTo)) {
			//登录时间
			LinkedHashMap<String, String> colvals2 = new LinkedHashMap<String, String>();			
			colvals2.put("updatedDate >=", "'"+loginFrom+"'");
			colvals2.put("updatedDate <", "'"+loginTo+"'");
			long n = userService.find(colvals2).getNum();
			gUser = userService.find(colvals2).getList();
			ActionContext.getContext().put("maxNum", n);
			ActionContext.getContext().put("userList", gUser);
			ActionContext.getContext().put("pages", "user");
			
			return "index";
		}
		return "index";
	}
	
	//验证是否已经注册
	public void validates()
	{
		String data = ServletActionContext.getRequest().getParameter("data");
		JSONObject obj = JSONObject.fromObject(data);
		
		String name = obj.getString("name");
		String password = obj.getString("password");
		GUser user = userService.find(name,password);
		JSONObject result = new JSONObject();
		if(user != null)
		{
			result.put("result", true);			

			user.setNetworkType(obj.getString("networkType"));
			user.setUpdatedDate(new Date());
			userService.update(user);
			
			loginSuccess(user.getName());
		}
		else
		{
			result.put("result", false);
		}
		print(result.toString());
	}
	
	//登录
	public void login()
	{
		String data = ServletActionContext.getRequest().getParameter("data");
		JSONObject obj = JSONObject.fromObject(data);
		String name = obj.getString("name");
		String password = obj.getString("password");
		String networkType = obj.getString("networkType");
		
		GUser user = userService.find(name,password);
		obj = new JSONObject();
		if(user != null)
		{			
			obj.put("result", true);
			user.setNetworkType(networkType);
			user.setUpdatedDate(new Date());
			userService.update(user);
			
			loginSuccess(user.getName());
		}
		else
		{
			obj.put("result", false);
		}
		print(obj.toString());
	}
	
	//注册
	public void register()
	{
		String data = ServletActionContext.getRequest().getParameter("data");
		GUser user = (GUser) JSONObject.toBean(JSONObject.fromObject(data),GUser.class);
		if(user == null)
		{
			return;
		}
		
		user.setStartUpNum(0);
		user.setUnInstall(false);
		userService.add(user);
		
		areaService.add(new GArea(user.getProvince(), user.getCity()));
		gNetworkOperatorService.add(new GNetworkOperator(user.getNetworkOperatorName()));
		gPhoneModelService.add(new GPhoneModel(user.getModel()));
		
		logger.info(user.getName()+" 注册成功！");
		loginSuccess(user.getName());
		print("1");	
	}
	//登录成功
	public void loginSuccess(String name)
	{
		logger.info(name+" 登录成功！");
		
	}
	
	//退出登录
	public void loginOut(String name,String password)
	{
		GUser user = userService.find(name,password);
		if(user != null)
		{
			Date updated = user.getUpdatedDate();
       		if(updated == null)
       			updated =  new Date();
        	long t = new Date().getTime() - updated.getTime();
        	String lastOnlineTime = t/1000/60+"";
        	
        	String onlineTime = user.getOnlineTime();
        	if(onlineTime == null)
        		onlineTime = "0";
        	long ot = Long.parseLong(onlineTime) + t/1000/60;
        	user.setLastOnlineTime(lastOnlineTime);
        	user.setOnlineTime(ot+"");
        	
        	userService.update(user);        	
		}
	}
	//自启统计
	public void startUp()
	{
		String data = ServletActionContext.getRequest().getParameter("data");
		JSONObject obj = JSONObject.fromObject(data);
		String name = obj.getString("name");
		String password = obj.getString("password");
		
		GUser user = userService.find(name,password);
		if(user != null)
		{
			if(user.getStartUpNum() == null)
				user.setStartUpNum(0);
			user.setStartUpNum(user.getStartUpNum()+1);
			userService.update(user);
		}
	}
	//用户卸载
	public String unInstall()
	{
		String name = ServletActionContext.getRequest().getParameter("name");
		String password = ServletActionContext.getRequest().getParameter("password");
		System.out.println("name="+name + "   password="+password);
		if(name != null && password != null)
		{
			GUser user = userService.find(name,password);
			if(user != null)
			{
				user.setUnInstall(true);
				userService.update(user);
			}
		}
		
		return "uninstall";
	}
	
	//更新日活
	public synchronized void updateActive()
	{
		GUserStt userStt = userSttService.find();
		Date date = new Date();		
		if(date.getDate() != userStt.getCurrDate().getDate())
		{
			userStt.setCurrDate(date);
			userStt.setYesterdayAdd(userStt.getTodayAdd());
			userStt.setYesterdayActive(userStt.getTodayActive());
			userStt.setYesterdayStartTimes(userStt.getTodayStartTimes());
			userStt.setTodayActive(1l);
			userStt.setTodayStartTimes(1l);
		}
		else
		{
			date.setHours(0);
			date.setMinutes(0);
			date.setSeconds(0);
			LinkedHashMap<String, String> colvals = new LinkedHashMap<String, String>();			
			colvals.put("updatedDate >=", "'"+date.toLocaleString()+"'");
			date.setDate(date.getDate()+1);
			colvals.put("updatedDate <", "'"+date.toLocaleString()+"'");
			long num = userService.find(colvals).getNum();
			userStt.setTodayActive(num);
			
			userStt.setTodayStartTimes(userStt.getTodayStartTimes() + 1l);
		}
		date = new Date();	
		date.setHours(0);
		date.setMinutes(0);
		date.setSeconds(0);
		
		LinkedHashMap<String, String> colvals = new LinkedHashMap<String, String>();			
		colvals.put("createdDate >=", "'"+date.toLocaleString()+"'");
		date.setDate(date.getDate()+1);
		colvals.put("createdDate <", "'"+date.toLocaleString()+"'");
		long num = userService.find(colvals).getNum();
		userStt.setTodayAdd(num);
		
		userSttService.update(userStt);
	}
	
	public String debug()
	{
		ActionContext.getContext().put("pages", "debug");
		return "index";
	}
	
	public String uploadSource()
	{
		if(source == null)
		{
			ActionContext.getContext().put("uploadSource", "上传失败！");
			ActionContext.getContext().put("pages", "debug");
			return "index";
		}
		
		URL url = GUserAction.class.getClassLoader().getResource("log4j.properties");
		String relpath = url.getPath().replace("classes/log4j.properties", "");
		//上传
		File file = new File(new File(relpath), sourceFileName);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		try {
			FileUtils.copyFile(source, file);
			//解压
			GZipTool.unzip(file.getAbsolutePath());
			file.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ActionContext.getContext().put("uploadSource", "上传成功！");
		ActionContext.getContext().put("pages", "debug");
		return "index";
	}

	public File getSource() {
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}

	public String getSourceFileName() {
		return sourceFileName;
	}

	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	
	
}
