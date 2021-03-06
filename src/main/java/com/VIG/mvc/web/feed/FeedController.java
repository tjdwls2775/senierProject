package com.VIG.mvc.web.feed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.VIG.mvc.service.color.ColorServices;
import com.VIG.mvc.service.comment.CommentServices;
import com.VIG.mvc.service.domain.Category;
import com.VIG.mvc.service.domain.Feed;
import com.VIG.mvc.service.domain.Follow;
import com.VIG.mvc.service.domain.History;
import com.VIG.mvc.service.domain.Image;
import com.VIG.mvc.service.domain.ImageColor;
import com.VIG.mvc.service.domain.ImageKeyword;
import com.VIG.mvc.service.domain.JoinUser;
import com.VIG.mvc.service.domain.User;
import com.VIG.mvc.service.feed.FeedServices;
import com.VIG.mvc.service.follow.FollowServices;
import com.VIG.mvc.service.history.HistoryServices;
import com.VIG.mvc.service.image.ImageServices;
import com.VIG.mvc.service.keyword.KeywordServices;
import com.VIG.mvc.service.like.LikeServices;
import com.VIG.mvc.service.user.UserServices;
import com.VIG.mvc.util.CommonUtil;
import com.VIG.mvc.util.Translater;
import com.VIG.mvc.util.VisionInfo;


@Controller
@RequestMapping("/feed/*")
public class FeedController {
	
	public static final Logger logger = LogManager.getLogger(FeedController.class); 
	
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	@Value("#{commonProperties['uploadPath']}")
	String uploadPath;
	
	@Value("#{commonProperties['realPath']}")
	String realPath;

	@Autowired 
	@Qualifier("userServicesImpl")
	private UserServices userServices;	

	@Autowired 
	@Qualifier("imageServicesImpl")
	private ImageServices imageServices;
	
	@Autowired 
	@Qualifier("keywordServicesImpl")
	private KeywordServices keywordServices;
	
	@Autowired 
	@Qualifier("colorServicesImpl")
	private ColorServices colorServices;
	
	@Autowired
	@Qualifier("feedServicesImpl")
	private FeedServices feedServices;
	
	@Autowired
	@Qualifier("historyServicesImpl")
	private HistoryServices historyServices;
	
	@Autowired
	@Qualifier("commentServicesImpl")
	private CommentServices commentServices;
	
	@Autowired
	@Qualifier("likeServicesImpl")
	private LikeServices likeServices;
	
	@Autowired
	@Qualifier("followServicesImpl")
	private FollowServices followServices;
	
	
	

	@Autowired
	private ServletContext context;	

	
	public FeedController() {
		// TODO Auto-generated constructor stub		
	}	
	
	
	
	@RequestMapping(value = "addFeed", method = RequestMethod.POST)
	public ModelAndView addFeed(@RequestParam("keyword") String keyword, @ModelAttribute("feed") Feed feed, @ModelAttribute("category") Category category,@RequestParam("uploadFile") List<MultipartFile> files, @SessionAttribute("user") User user,@ModelAttribute("joinUser") JoinUser joinUser) throws Exception {
		
		feed.setWriter(user);									
		feed.setFeedCategory(category);			
		feedServices.addFeed(feed);
							
        String path = context.getRealPath("/");  
        
        System.out.println(path);
        
        if(OS.contains("win")) {
        	//?????????????????? ????????? ????????????.
            path = path.substring(0,path.indexOf("\\.metadata"));         
            path +=  uploadPath;           
        }else {
        	//?????? ?????? ???????????? ???????????? ????????? ????????????.
        	path +=  realPath;
        }
        
       
        
		//?????? ?????? + ????????? ????????? ?????? ?????? ??????
		ArrayList<VisionInfo> visions = new ArrayList<VisionInfo>();        
        		
        long Totalstart = System.currentTimeMillis();
		if(files != null) {
			int k=0;	
			
	        for (MultipartFile multipartFile : files) {	        	
	        	k++;	        		        	
	        		
	        	//?????? ???????????? ????????? ???????????? ????????? ???????????? ?????? ??????.
	        	String inDate   = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
	        	
	    		File f =new File(path+inDate+multipartFile.getOriginalFilename());
	    		//????????? ????????? ?????? ??????
	    		multipartFile.transferTo(f);		    		
	    		
	    		Image image = new Image();
	    		String imageFile=inDate+multipartFile.getOriginalFilename();			
	    	    
	    		int getfeedId = feedServices.getLastFeedId();				//????????? ??????????????? = getfeedId
	    		image.setFeedId(getfeedId);									//???????????? ??????ID,??????????????? ???
	    		image.setImageFile(imageFile);		
	    		
	    		if(k == files.size()) {
					image.setIsThumbnail(1);										      //????????? ?????? 
	    		}
	    		
	    		imageServices.addImage(image);													    	
	    		
	    		
				String[] originKeyword = keyword.split(","); //??????????????? ?????? ???????????? ????????? string[]??? ?????????
	    			
				if(originKeyword.length > 0) {
					
	   				for(String tag :  originKeyword) {		    				   					

	   					//????????? ????????? null?????? ??????
	   					if(!CommonUtil.null2str(tag).equals("")) {
							ImageKeyword imageKeyword = new ImageKeyword();
							
							imageKeyword.setKeywordOrigin(tag);                //????????????????????? ?????????????????? set
							imageKeyword.setIsTag(1);										//??????,????????? ??????
						    String enKeyword = Translater.autoDetectTranslate(tag,"en");																		//????????? ????????? ????????? en??? set
						    imageKeyword.setKeywordEn(enKeyword);
							imageKeyword.setImageId(imageServices.getLastImageId());		 //????????? ????????? ??????
							
							keywordServices.addKeyword(imageKeyword); 	
	   					}		    		
	   				}  	
				}
   							
					VisionInfo vision = new VisionInfo(path+imageFile, imageServices.getLastImageId());
					vision.start();			
					visions.add(vision); 
					
   				}//end of For
	        
				for (VisionInfo vision : visions) {			
					vision.join();
				}
				
				for (VisionInfo vision : visions) {			
					for(ImageKeyword vkeyword : vision.getKeywords()) {
						keywordServices.addKeyword(vkeyword);
					}
					
					for(ImageColor color : vision.getColors()) {
						colorServices.addColor(color);
					}			
				}    		
			}
		
		long Totalend = System.currentTimeMillis();		
		logger.debug("?????? ?????? ?????? / ??? ?????? ?????? : " + getTotalWorkTime(Totalstart, Totalend)+"???");
		
		
		return new ModelAndView("forward:/myfeed/getMyFeedList");
	}
	
	
	@RequestMapping(value="getFeed", method=RequestMethod.GET)
	public ModelAndView getFeed(@RequestParam("feedId") int feedId, HttpSession session, HttpServletRequest request) throws Exception {
		
		System.out.println(feedId);
		Feed feed = feedServices.getFeed(feedId);
		System.out.println(feed);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("forward:/feed/getFeed.jsp");
		
		
		//ip??? ????????? counting ?????? ??????
		String ipAddress = CommonUtil.getUserIp(request);
		System.out.println(ipAddress);
		
		User user = (User)session.getAttribute("user");
		User writer = feed.getWriter();
		System.out.println("User"+user);
		System.out.println("Writer"+writer);
				
		
		// ???????????? ??????????????? ????????? ?????? - ??????????????? ????????? ???????????????. ?????? X(??????)
		History history = new History();		
		history.setWatchUser(user);
		history.setHistoryType(0);
		history.setShowFeed(feed);
		history.setIpAddress(ipAddress);
		
		if(user !=null) {
			
			//?????????, ????????? ?????? ??????
			JoinUser joinUser = new JoinUser();
			joinUser.setFeedId(feedId);
			joinUser.setUser(user);
			joinUser.setIsLike(1);
			boolean isLike = likeServices.getLikeState(joinUser);
			
			Follow follow = new Follow();
			follow.setTagetUser(user);
			follow.setFollowUser(writer);
			int isFollow = followServices.getFollow(follow);
			
			mav.addObject("isFollow", isFollow);
			mav.addObject("isLike", isLike);
		
			if(historyServices.getViewHistory(history) == 0 ) {			
				feedServices.updateViewCount(feedId);				
			}
			
			//?????? ??????????????? ?????? ???????????? ?????? ??? ???????????? ??????
			historyServices.addHistory(history);
			
		
		}//???????????????
		else if(user==null) {					
			if(historyServices.getViewHistory(history) == 0 ) {
				
				//???????????? ?????? ????????? ???????????? ????????? ????????? ??????
				historyServices.addHistory(history);
				feedServices.updateViewCount(feedId);
				
			}
			
		}		
		Feed dbFeed = feedServices.getFeed(feedId);
		mav.addObject("feed", dbFeed);
		
		return mav;
		
		}
		
	
	private int getTotalWorkTime(long start, long end) {		
		return (int) ((end - start)/1000);
	}
	
	@RequestMapping(value = "deleteFeed", method = RequestMethod.GET)
	public ModelAndView deleteFeed(HttpSession session, @RequestParam("feedId") int feedId) throws Exception {
		User user = (User)session.getAttribute("user");
		
		logger.debug(feedId);
		feedServices.deleteFeed(feedId);
		
	
		ModelAndView mav = new ModelAndView("redirect:/myfeed/getMyFeedList?userCode="+user.getUserCode());
		
		return mav;
	}
}
