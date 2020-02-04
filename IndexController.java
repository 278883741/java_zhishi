package com.imooc.controller;

import com.imooc.model.SysPermission;
import com.imooc.model.SysUser;
import com.imooc.service.CardService;
import com.imooc.service.UserService;
import com.imooc.utils.MD5Utils;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    UserService userService;
    @Autowired
    CardService cardService;



    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;


    /**
     * 定义流程
     */
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public void test(){
        // 1.获取默认的processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 2.将定义好的流程文件部署到数据库中
        // 2.1加载资源文件 act_re_deployment -- act_re_procdef -- act_ge_bytearray;
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .addClasspathResource("processes/holiday.bpmn")
                .addClasspathResource("processes/holiday.png")
//                .key("holiday")
                .name("请假申请流程")
                .deploy();
        // act_re_procdef 表对应的是流程定义 - bpmn
        // act_re_deployment 表对应的是部署，信息就是上面代码的信息
    }

    /**
     * 启动流程实例
     */
    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    @ResponseBody
    public void test1(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        // 流程变量，是在流程线上的 - act_ru_variable act_ge_bytearray
        Map<String,Object> map = new HashMap<>();
        map.put("num",3);
        // act_re_procdef表中的key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holiday","businessid",map);
        /*
            act_hi_actinst -- 已完成的活动信息 end_time
            act_hi_identitylink -- 参与者信息？
            act_hi_procinst -- 流程实例
            act_hi_taskinst -- 任务实例？
            act_ru_execution -- 执行表？
            act_ru_identitylink - 参与者信息？
            act_ru_task -- 任务？
        */

        /*
            通过表达式的方式制定审批人，真实场景类似转包中的选人，再bpmn文件中写${assignee}
            Map<String,Object> map = new HashMap<>();
            map.put("assignee","xiaohong");
            runtimeService.startProcessInstanceByKey("holiday","businessId",map);
        */
    }

    /**
     * 获取我的待办
     */
    @RequestMapping(value = "/test2", method = RequestMethod.GET)
    @ResponseBody
    public void test2(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();

        /*
            再待办的时候设置流程变量
            Map<String,Object> map1 = new HashMap<>();
            map1.put("num",3);
            taskService.setVariables("taskId",map1);
        */

        // holiday -- 流程定义中的key
//        List<Task> list = taskService.createTaskQuery()
//                .processDefinitionKey("holiday")
//                .taskAssignee("xiaozong")
//                .list();
//        for (Task item : list){
//            // 审批完成时给流程变量赋值
//            Map<String,Object> map = new HashMap<>();
//            map.put("num",3);
//            // 审批 -- act_hi_taskinst
//            taskService.complete(item.getId(),map);
//        }
    }

    /**
     * 获取所有流程
     */
    @RequestMapping(value = "/test3", method = RequestMethod.GET)
    @ResponseBody
    public void test3(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> list = processDefinitionQuery.processDefinitionKey("holiday")
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        // 删除流程定义，不会删除历史记录，只会操作流程定义时候操作的那几张表
        // 假如一个流程正在跑，所有流程都跑完了可以删，不影响历史记录，如果有的流程跑到一半，删流程是不允许的，那么下方法就要加true，把对应的审批记录也都删除了
        repositoryService.deleteDeployment("2511",true);
    }

    /**
     * 查询历史记录
     */
    @RequestMapping(value = "/test4", method = RequestMethod.GET)
    @ResponseBody
    public void test4(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        HistoryService historyService = processEngine.getHistoryService();
        HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
        query.processInstanceId("");
        List<HistoricActivityInstance> list = query.orderByHistoricActivityInstanceStartTime().asc().list();
    }

    /**
     * 挂起或激活
     */
    @RequestMapping(value = "/test5", method = RequestMethod.GET)
    @ResponseBody
    public void test5(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("holiday").singleResult();
        boolean suspended = processDefinition.isSuspended();
        if(suspended){
            repositoryService.activateProcessDefinitionById(processDefinition.getId());
        }else{
            // 挂起的话不允许创建新的流程，未跑完的流程也不能继续往下走了
            repositoryService.suspendProcessDefinitionById(processDefinition.getId());
        }
    }

    /**
     * 通过流程实例Id设置流程变量
     */
    @RequestMapping(value = "/test6", method = RequestMethod.GET)
    @ResponseBody
    public void test6(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        String insId = "";
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String,Object> map = new HashMap<>();
        map.put("num",3);
        runtimeService.setVariables(insId,map);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model,HttpSession session) {
        SysUser user = userService.getLoginUser();
        model.addAttribute("occupationPercent",cardService.getOccupationPercent());
        return "index";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model) {
        return "login";
    }

    @RequestMapping(value = "/checkLogin", method = RequestMethod.POST)
    public String checkLogin(String userName, String password,boolean rememberMe, HttpSession session, RedirectAttributes redirectAttributes) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(userName, MD5Utils.getMD5Str(password));
        if(rememberMe){
            token.setRememberMe(true);
        }
        Boolean loginStatus = false;

        try {
            // 在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            // 每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            // 所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.info("对用户[" + userName + "]进行登录验证..验证开始");
            subject.login(token);
            loginStatus = true;
            token.clear();

            logger.info("对用户[" + userName + "]进行登录验证..验证通过");
        } catch (UnknownAccountException uae) {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,未知账户");
            redirectAttributes.addFlashAttribute("message", "未知账户");
        } catch (IncorrectCredentialsException ice) {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,错误的凭证");
            redirectAttributes.addFlashAttribute("message", "密码不正确");
        } catch (LockedAccountException lae) {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,账户已锁定");
            redirectAttributes.addFlashAttribute("message", "账户已锁定");
        } catch (ExcessiveAttemptsException eae) {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,错误次数过多");
            redirectAttributes.addFlashAttribute("message", "用户名或密码错误次数过多");
        } catch (AuthenticationException ae) {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,堆栈轨迹如下");
            redirectAttributes.addFlashAttribute("message", "用户名或密码不正确");
        }

        //验证是否登录成功
        if (loginStatus) {
            SysUser user = (SysUser) subject.getPrincipal();
            session.setAttribute("user", user);
            List<String> roles = userService.getRoleByUserId(user.getId());
            List<SysPermission> permissions = userService.getPermissionByUserId(user.getId());

            List<String> list_permissions = new ArrayList<>();
            for (SysPermission item: permissions) {
                list_permissions.add(item.getPermissionName());
            }

            logger.info("登录用户:" + userName);
            logger.info("登录角色:" + roles.toString());
            logger.info("登录权限:" + list_permissions.toString());

            session.setAttribute("roles",roles.toString());
            session.setAttribute("permissions",permissions);
            session.setAttribute("list_permissions",list_permissions.toString());
            return "redirect:/";
        } else {
            token.clear();
            return "redirect:/login";
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public void logout(){
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
    }
}
