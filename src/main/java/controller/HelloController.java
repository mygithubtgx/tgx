package controller;

import controller.test1.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("")
    public String Hello(User user){
        return user.toString();
    }

}
