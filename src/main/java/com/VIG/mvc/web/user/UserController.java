
package com.VIG.mvc.web.user;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.VIG.mvc.service.domain.GoogleProfile;
import com.VIG.mvc.service.domain.Page;
import com.VIG.mvc.service.domain.Search;
import com.VIG.mvc.service.domain.Token;
import com.VIG.mvc.service.domain.User;
import com.VIG.mvc.service.report.ReportServices;
import com.VIG.mvc.service.user.UserServices;
import com.VIG.mvc.util.CommonUtil;
import com.VIG.mvc.web.event.EventController;
import com.google.gson.Gson;

@Controller
@RequestMapping("/user/*")
public class UserController {
	
	public static final Logger logger = LogManager.getLogger(EventController.class); 
	
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	@Autowired
	BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private ServletContext context;	
	
	@Autowired 
	@Qualifier("userServicesImpl")
	private UserServices userServices;
	
	@Autowired
	@Qualifier("reportServicesImpl")
	private ReportServices reportService; 
		
	@Value("#{commonProperties['uploadPath']}")
	String uploadPath;
	
	@Value("#{commonProperties['realPath']}")
	String realPath;
	
	@Value("#{commonProperties['otherPath']}")
	String otherPath;
	
	
	
	@Value("#{commonProperties['currentDate'] ?: 30}")
	int currentDate;
	
	@Value("#{commonProperties['pageSize'] ?: 5}")
	int pageSize;
	
	@Value("#{commonProperties['pageUnit'] ?: 5}")
	int pageUnit;
	
	public UserController() {
	}	
	
	
	//?????? ????????? ??????	
	@RequestMapping(value="googleLogin")
	public ModelAndView googleLogin( @RequestParam("code") String code, Model model, HttpSession session) throws Exception{
		String query = "code=" + code;
		query += "&client_id=" + "153642235365-kg8rl6mmbhk7rh4tuefg790id1llegd1.apps.googleusercontent.com";
		query += "&client_secret=" + "NeMh3KQqpOB7m6xI6Lo3ugxD";
		
		//?????? os??? ??????????????? ????????? ??????
		if(OS.contains("win")) {
			query += "&redirect_uri=" + "http://localhost:8080/VIG/user/googleLogin";	
		}else {
			query += "&redirect_uri=" + "http://ec2-13-125-196-55.ap-northeast-2.compute.amazonaws.com:8080/VIG/user/googleLogin";
		}		
		
		query += "&grant_type=authorization_code";

		String tokenJson = CommonUtil.getHttpConnection("https://accounts.google.com/o/oauth2/token", query);	
		
		Gson gson = new Gson();
		Token token = gson.fromJson(tokenJson, Token.class);
		String ret = CommonUtil.getHttpConnection("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + token.getAccess_token());	
		
		GoogleProfile profile = gson.fromJson(ret, GoogleProfile.class);
		
		//???????????? ???????????? ??????		
		User checkUser = ((User)session.getAttribute("user"));
		
		if(checkUser != null) {		
			
			//?????? ?????? ????????? ???????????? ????????? ?????? ??????
			if(userServices.getGoogleID(profile.getId()) != null) {				
				
				return new ModelAndView("forward:/common/alertView.jsp", "message", "?????? ????????? ????????? ????????????.");
			} 
			
			//???????????? ???????????? ?????? ????????? ???????????? -> ???????????? ??????????????? ?????? DB?????? ?????? ????????? ?????? ??????			
			User userTemp = userServices.getUserOne(checkUser.getUserCode());
			
			userTemp.setGoogleId(profile.getId());
			
			userServices.updateUser(userTemp);
			
			//????????? ????????? ????????? ????????????
			session.setAttribute("user", userTemp);
			
			return new ModelAndView("redirect:/main/VIG");
			
		}else {
			
			//????????? ????????? ????????? ????????? ????????? ?????????
			if(userServices.getGoogleID(profile.getId()) != null ) {
				
				User user =  userServices.getGoogleID(profile.getId());				
				
				if(user.getState() == 0 ) {
					session.setAttribute("user",user);							
					return new ModelAndView("redirect:/main/VIG");
					
				}else if(user.getState() == 3 || user.getState() == 4){		
					String msg = "????????? ??? ?????? ??????????????????";				
					return new ModelAndView("forward:/common/alertView.jsp", "message", msg);
				}else {	
					
					String msg = "????????????<br/>????????? ????????? ????????? ??????????????????.<br/>?????? ????????? ?????? : " + user.getBanDate().toString() +"??????";				
					return new ModelAndView("forward:/common/alertView.jsp", "message", msg);				
					
				}		

			}
			
			//?????? ????????? ????????? ????????? ?????? ???????????? ??????
			User user = new User();
			user.setUserCode("user"+userServices.getLastUserNum());
			user.setEmail(profile.getEmail());
			user.setGoogleId(profile.getId());				
			
			model.addAttribute("user", user);
		}
		
		return new ModelAndView("forward:/user/addUserView.jsp");		
	
	}
	

//=========????????????===========================================================//    ?????? ????????????!
	
	@RequestMapping(value="addUserView", method=RequestMethod.GET)
	public ModelAndView addUser() throws Exception{		
	
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("forward:/user/addUserView.jsp");	
		return modelAndView;
	}

	@RequestMapping( value="addUser", method=RequestMethod.POST )
	public String addUser(@ModelAttribute("user") User user, HttpSession session ) throws Exception {
		
		String pwdBycrypt = passwordEncoder.encode(user.getPassword());
	    user.setPassword(pwdBycrypt);
		userServices.addUser(user);
		
		session.setAttribute("user", userServices.getUserOne(user.getUserCode()));		
		return "redirect:/main/VIG";
	}
			
//====id ?????? =====
	
	@RequestMapping( value="checkDuplication", method=RequestMethod.POST )
	public String checkDuplication( @RequestParam("userCode") String userCode , Model model ) throws Exception{		
		
		//Business Logic
		boolean result=userServices.checkDuplication(userCode);
		// Model ??? View ??????
		model.addAttribute("result", new Boolean(result));
		model.addAttribute("userCode", userCode);

		return "forward:/user/checkDuplication.jsp";
	}

//=======?????????=====
		
	@RequestMapping( value="login", method=RequestMethod.GET)
	public ModelAndView login() throws Exception{			

		ModelAndView model = new ModelAndView();
		model.setViewName("login");		
		return model;
	}

	@RequestMapping( value="login", method=RequestMethod.POST )
	public ModelAndView login(@ModelAttribute("user") User user, HttpSession session) throws Exception{
				
		User dbUser = userServices.getUserOne(user.getUserCode());
		ModelAndView mv = new ModelAndView();
		
		if(dbUser == null) {
			String msg = "???????????? ?????? ?????? ??????????????????.";				
			return new ModelAndView("forward:/common/alertView.jsp", "message", msg);
		}
		
		if (BCrypt.checkpw(user.getPassword(), dbUser.getPassword())){				
			
			if(dbUser.getState() == 0) {
				session.setAttribute("user", dbUser);		
				mv.setViewName("redirect:/main/VIG");
						
				return mv;					
			}else if(dbUser.getState() == 3 || dbUser.getState() == 4){		
				String msg = "????????? ??? ?????? ??????????????????";				
				return new ModelAndView("forward:/common/alertView.jsp", "message", msg);
			}				
			
				String msg = "????????????<br/>????????? ????????? ????????? ??????????????????.<br/>?????? ????????? ?????? : " + dbUser.getBanDate().toString() +"??????";				
				return new ModelAndView("forward:/common/alertView.jsp", "message", msg);
				
		} else {
			mv.setViewName("forward:/user/loginView.jsp");		
			mv.addObject("msg", "fail");
			return mv;
		}
		
	}


//=======????????????
	
	@RequestMapping( value="logout", method=RequestMethod.GET)
	public ModelAndView logout(HttpSession session) throws Exception{
					
			//?????? ????????? ?????????
			session.removeAttribute("user");		
		
			//????????? ??????????????? ????????? ??????
			session.invalidate();
			
		return new ModelAndView("redirect:/main/VIG");
	}

	
	//====???????????? ?????? nav	
	@RequestMapping( value="updateUser", method=RequestMethod.GET )
	public ModelAndView updateUser(@RequestParam(value="uesrCode", required=false) String userCode,HttpSession session, User user)throws Exception{ 
	
		User writer = userServices.getUserOne(user.getUserCode());
		session.setAttribute("writer", writer);			
		ModelAndView mv = new ModelAndView();
		mv.setViewName("forward:/user/updateUser.jsp");
		mv.addObject("user", writer);
		return mv;
	}
	
	@RequestMapping( value="updateUser", method=RequestMethod.POST )
	public ModelAndView updateUser(@ModelAttribute("user") User user, @RequestParam("uploadFile") MultipartFile file, HttpSession session )throws Exception{ 
		
        String path = context.getRealPath("/");  

        System.out.println(path);
        
        User dbuser = userServices.getUserOne(user.getUserCode());
        
        if(OS.contains("win")) {
        	//?????????????????? ????????? ????????????.
            path = path.substring(0,path.indexOf("\\.metadata"));         
            path +=  uploadPath;           
        }else {
        	//?????? ?????? ???????????? ???????????? ????????? ????????????.
        	path +=  realPath;
        }
        
		if(!file.isEmpty()) {
			
        	//?????? ???????????? ????????? ???????????? ????????? ???????????? ?????? ??????.	        	
        	String inDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());		
      	       	
        	File f=new File(path+inDate+file.getOriginalFilename());
        	file.transferTo(f);
			user.setProfileImg(f.getName());	        	
	        
		}else {
			
			user.setProfileImg(dbuser.getProfileImg());
			
		}
		//??????????????? ?????? ??????
		if(user.getPassword()!= null || user.getPassword()!="") {
			
			String pwdBycrypt = passwordEncoder.encode(user.getPassword());
		    user.setPassword(pwdBycrypt);
			
		}
		
		userServices.updateUser(user);	
		session.setAttribute("user", userServices.getUserOne(user.getUserCode()));

		ModelAndView mv = new ModelAndView();
		mv.addObject("user", userServices.getUserOne(user.getUserCode()));
		mv.setViewName("redirect:/myfeed/getMyFeedList?userCode="+user.getUserCode());
		
		return mv;
	        
	}	
	
	//
	@RequestMapping( value="getUser", method=RequestMethod.GET )
	public String getUser( @RequestParam("userCode") String userCode , Model model ) throws Exception {		

		User user = userServices.getUserOne(userCode);
		model.addAttribute("user", user);		
		return "forward:/user/getUser.jsp";
	}
	
	//=====?????? ????????? nav
	

	@RequestMapping(value="getUserList" )
	public String getUserList( @ModelAttribute("search") Search search, Model model, HttpSession session) throws Exception{		
		
		
		User admin = (User)session.getAttribute("user");		
		
		if(admin == null) {
			
			model.addAttribute("message", "???????????? ???????????????.");
			return "forward:/common/alertView.jsp";
		}else if(!admin.getRole().equals("admin")) {
			model.addAttribute("message", "???????????? ?????? ???????????????.");
		}		
		
		
		if(search.getCurrentPage() ==0 ){
			search.setCurrentPage(1);
		}
		if(search.getKeyword() == null) {
			search.setKeyword("");
		}
		search.setCurrentDate(currentDate);
		search.setPageSize(pageSize);
		
		// Business logic ??????
		Map<String , Object> map=userServices.getUserList(search);
		
		Page resultPage = new Page( search.getCurrentPage(), ((Integer)map.get("totalCount")).intValue(), pageUnit, pageSize);
	

		// Model ??? View ??????
				model.addAttribute("list", map.get("list"));
				model.addAttribute("resultPage", resultPage);
				model.addAttribute("search", search);
				model.addAttribute("map", map);
				model.addAttribute("writer", admin);
			
				
		return "forward:/user/getUserList.jsp";
	}
	
	
	
	
	//======??????==================================//
		
		@RequestMapping(value = "deleteUser", method = RequestMethod.GET)
		public ModelAndView deleteUser(HttpSession session) throws Exception {
			
			User user = userServices.getUserOne(((User)session.getAttribute("user")).getUserCode());
			
			ModelAndView mv = new ModelAndView();		
			mv.addObject("user", user);
			mv.setViewName("forward:/user/deleteUser.jsp");
			
			return mv;
		}
		
	
		@RequestMapping (value="deleteUser", method = {RequestMethod.GET, RequestMethod.POST})
		public ModelAndView deleteUser(User user,HttpSession session) throws Exception{			
	
			ModelAndView mv = new ModelAndView();
			userServices.deleteUser(user);
			session.invalidate();
			mv.setViewName("redirect:/");
			return mv;
		}	
		

}
