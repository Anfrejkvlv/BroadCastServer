package com.emma.broadcastserver;

import com.emma.broadcastserver.client.ClientService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class BroadCastServerApplication {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: java -jar BroadCastServer-1.jar [start| connect]");
        }

        String command = args[0];

        if (command.equalsIgnoreCase("start")) {
            SpringApplication.run(BroadCastServerApplication.class, args);
            System.out.println("BroadCastServer démarré sur le port:8085... ");

        }else if (command.equalsIgnoreCase("connect")) {
            SpringApplication app=new SpringApplication(BroadCastServerApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            ApplicationContext context = app.run(args);

            ClientService clientService=context.getBean(ClientService.class);
            clientService.startClient("ws://localhost:8085/broadcast");
        }
        else {
            System.out.println("Command inconnue: " + command);
        }
    }
}
