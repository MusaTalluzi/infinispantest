package org.redhatsummit.infinispan.rest;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.crypto.Mac;

import org.redhatsummit.infinispan.domain.MachineComponent;

public class MachineManager {

    private static final String INFINISPAN_HOST = "infinispan.host";
    // REST specific properties
    public static final String HTTP_PORT = "infinispan.http.port";
    public static final String REST_CONTEXT_PATH = "infinispan.rest.context.path";
    private static final String PROPERTIES_FILE = "infinispan.properties";
    private static final String cacheName = "components";

    private static final String initialPrompt = "Choose action:\n" + "============= \n"
            + "ac  -  add a component\n"
            + "sa  -  set attrition\n"
            + "rc  -  remove a component\n"
            + "p   -  print all components\n"
            + "q   -  quit\n";

    private BufferedReader br;
    private RESTCache<String, Object> cache;

    public MachineManager(BufferedReader br) {
        this.br = br;
        cache = new RESTCache<>(cacheName, "http://" + infinispanProperty(INFINISPAN_HOST) + ":" + infinispanProperty(HTTP_PORT)
                + infinispanProperty(REST_CONTEXT_PATH));
    }

    public void addMachineComponent() {
        System.out.print("Enter component id: ");
        Long componentId = null;
        try {
            componentId = Long.parseLong(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        double initialAttrition = 0.0;
        List<String> components = (List<String>) cache.get("components");
        if (components == null) {
            components = new ArrayList<>();
        }
        MachineComponent component = new MachineComponent(componentId, initialAttrition);
        cache.put(componentId.toString(), component);
        components.add(componentId.toString());
        cache.put("components", components);
    }

    public void setAttrition() {
        System.out.print("Enter component id: ");
        try {
            String idAsString = br.readLine();
            MachineComponent component = (MachineComponent) cache.get(idAsString);
            if (component != null) {
                System.out.print("Enter attrition: ");
                component.setAttrition(Double.parseDouble(br.readLine()));
                cache.put(idAsString, component);
            } else {
                System.out.println("Component with id " + idAsString + " does not exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void removeComponent() {
        System.out.print("Enter component id: ");
        try {
            String idAsString = br.readLine();
            MachineComponent component = (MachineComponent) cache.get(idAsString);
            if (component != null) {
                cache.remove(idAsString);
                List<String> components = (List<String>) cache.get("components");
                if (components != null) {
                    while (components.contains(idAsString)) {
                        components.remove(idAsString);
}
                }
                cache.put("components", components);
            } else {
                System.out.println("Component with id " + idAsString + " does not exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printComponents() {
        List<String> components = (List<String>) cache.get("components");
        if (components != null) {
            for (String component : components) {
                System.out.println(cache.get(component).toString());
            }
        }
    }

    public static String infinispanProperty(String name) {
        Properties props = new Properties();
        {
            try {
                props.load(MachineComponent.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return props.getProperty(name);
        }
    }

    public static void main(String[] args) {
        MachineManager manager = new MachineManager(new BufferedReader(new InputStreamReader(System.in)));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(initialPrompt);

        while (true) {
            System.out.print("> ");
            String action = null;
            try {
                action = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch (action) {
                case "ac":
                    manager.addMachineComponent();
                    break;
                case "sa":
                    manager.setAttrition();
                    break;
                case "rc":
                    manager.removeComponent();
                    break;
                case "p":
                    manager.printComponents();
                    break;
                case "q":
                    return;
                default:
                    break;
            }
        }
    }
}
