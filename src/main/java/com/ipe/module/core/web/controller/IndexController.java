package com.ipe.module.core.web.controller;

import static org.apache.shiro.SecurityUtils.getSubject;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ipe.module.bpm.service.ProcessTaskService;
import com.ipe.module.core.service.MessageService;
import com.ipe.module.core.service.NoticeService;
import com.ipe.module.core.service.UserService;
import com.ipe.module.core.web.security.CustUsernamePasswordToken;
import com.ipe.module.core.web.security.SystemRealm;
import com.ipe.module.core.web.util.RestRequest;
import com.ipe.module.core.web.util.WebUtil;

/**
 * Created with IntelliJ IDEA.
 * User: tangdu
 * Date: 13-9-7
 * Time: 下午10:21
 * To change this template use File | Settings | File Templates.
 */
@Controller
public class IndexController extends AbstractController {

    @Autowired
    private UserService userService;
    private static final Logger Logger = LoggerFactory.getLogger(IndexController.class);

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/signin", method = RequestMethod.GET)
    public String signin() {
        return "signin";
    }

    /**
     * 根据url跳转到不同URL
     *
     * @param url
     * @return
     */
    @RequestMapping(value = "/reui/${url}", method = RequestMethod.GET)
    public String reUI(@PathVariable String url) {
        return url;
    }


    /**
     * 用户登录
     *
     * @param useraccount
     * @param password
     * @param request
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@RequestParam String useraccount,
                        @RequestParam String password, HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        CustUsernamePasswordToken token = new CustUsernamePasswordToken(useraccount, password,
                WebUtil.getIpAddr(request),
                request.getMethod(),
                request.getParameter("captcha"),
                WebUtil.getIpAddr(request));
        try {
            getSubject().login(token);
            return "redirect:/";
        } catch (UnknownAccountException uae) {
            redirectAttributes.addAttribute("errorMsg", "用户名不存在系统！");
        } catch (IncorrectCredentialsException ice) {
            redirectAttributes.addAttribute("errorMsg", "密码错误！");
        } catch (LockedAccountException lae) {
            redirectAttributes.addAttribute("errorMsg", "用户已经被锁定不能登录，请与管理员联系！");
        } catch (ExcessiveAttemptsException eae) {
            redirectAttributes.addAttribute("errorMsg", "错误次数过多！");
        } catch (AuthenticationException ae) {
            redirectAttributes.addAttribute("errorMsg", "其他的登录错误！");
            ae.printStackTrace();
        }
        return "redirect:signin";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        request.getSession().removeAttribute("userPrvLoginTime");//TODO 上次登录时间
        try {
            userService.logout();
            getSubject().logout();
        } catch (Exception e) {
            Logger.error("Exception {}", e);
        }
        return "signin";
    }

    @Autowired
    private ProcessTaskService taskService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private MessageService messageService;
    
    private static final String PATH="tools/";
    /**
     * 首页展示
     * @param request
     * @param data
     * @return
     */
    @RequestMapping(value = "/getIndexView")
    public String getIndexView(HttpServletRequest request,ModelMap data) {
		 //step1:用户信息
		 SystemRealm.UserInfo userInfo=((SystemRealm.UserInfo) SecurityUtils.getSubject().getPrincipal());
		 data.put("userInfo", userInfo);
		 //step2:任务信息
		 data.put("mtaskLists",taskService.userTaskList(null, new RestRequest()).getRows());
		 //step3:最新公告
		 data.put("noticeLists", noticeService.listAll());
		 //step4:最新消息
		 data.put("messageLists", messageService.listAll());
		 //step5:内存信息
		 MemoryMXBean my = ManagementFactory.getMemoryMXBean();
		 data.put("initMemory", my.getHeapMemoryUsage().getInit() / 1000000 + " M");
		 data.put("maxMemory", my.getHeapMemoryUsage().getMax() / 1000000 + " M");
		 data.put("usedMemory", my.getHeapMemoryUsage().getUsed() / 1000000 + " M");
		 OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		 data.put("osName", os.getName());
		 RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
		 data.put("vmName", rt.getVmName());
		 return PATH+"indexview_ftl";
    }
    
    /**
     * 公共文件下载方法
     *
     * @param filePath
     * @param response
     */
    @RequestMapping(value = {"/downFile"})
    public void downlogs(@RequestParam String filePath, HttpServletResponse response) {
        try {
            filePath = URLDecoder.decode(filePath, "UTF-8");
            File file = new File(filePath);
            if (file.canRead() && file.exists()) {
                super.downFile(file, response);
            }else{
                super.downFileError(response);
            }
        } catch (Exception e) {
            Logger.error("del error", e);
            super.downFileError(response);
        }
    }
}
