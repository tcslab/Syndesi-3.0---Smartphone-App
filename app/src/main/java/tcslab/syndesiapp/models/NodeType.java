package tcslab.syndesiapp.models;

import tcslab.syndesiapp.R;

/**
 * Contains the different types of sensors and their respective units, images and statuses.
 *
 * Created by Blaise on 31.05.2015.
 */
public enum NodeType {
    bulb{
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_bulb_on;
            }else{
                return R.drawable.node_bulb_off;
            }
        }
    },
    light{
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_light_on;
            }else{
                return R.drawable.node_light_off;
            }
        }
    },
    curtain{
        @Override
        public int getIcon(String status){
            if(status.equals("up")){
                return R.drawable.node_curtain_on;
            }else{
                return R.drawable.node_curtain_off;
            }
        }
        @Override
        public String getStatus(String status){
            if(status.equals("up")){
                return "up";
            }else{
                return "down";
            }
        }
        @Override
        public String getToggleStatus(String status){
            if(status.equals("up")){
                return "down";
            }else{
                return "up";
            }
        }
    },
    coffee{
        @Override
        public int getIcon(String status){
            if(status.equals("up")){
                return R.drawable.node_coffee_on;
            }else{
                return R.drawable.node_coffee_off;
            }
        }
    },
    alarm{
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_alarm_on;
            }else{
                return R.drawable.node_alarm_off;
            }
        }
    },
    door{
        @Override
        public int getIcon(String status){
            if(status.equals("up")){
                return R.drawable.node_door_on;
            }else{
                return R.drawable.node_door_off;
            }
        }
    },
    heater{
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_heater_on;
            }else{
                return R.drawable.node_heater_off;
            }
        }
    },
    fan{
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_fan_on;
            }else{
                return R.drawable.node_fan_off;
            }
        }
    },
    sengen{
        public String getStatus(String status){
            if(status.equals("1")){
                return "on";
            }else{
                return "off";
            }
        }
        @Override
        public int getIcon(String status){
            if(status.equals("on")){
                return R.drawable.node_bulb_on;
            }else{
                return R.drawable.node_bulb_off;
            }
        }
    },
    generic;

    public static NodeType getType(String device){
        if (device.contains("bulb")) {
            return NodeType.bulb;
        } else if (device.contains("curt")) {
            return NodeType.curtain;
        } else if (device.contains("light")) {
            return NodeType.light;
        } else if (device.contains("coffee")) {
            return NodeType.coffee;
        } else if (device.contains("alarm")) {
            return NodeType.alarm;
        }else if (device.contains("door")) {
            return NodeType.door;
        }else if (device.contains("fan")) {
            return NodeType.fan;
        }else if (device.contains("heater")) {
            return NodeType.heater;
        }else if (device.contains("sengen") || device.substring(0, 2).equals("SG")) {
            return NodeType.sengen;
        }else{
            return NodeType.generic;
        }
    }

    public static String parseResponse(String response){
        if(response.contains("ON")) {
            return "on";
        }else if(response.contains("OFF")){
            return "off";
        }else if(response.contains("1")) {
            return "on";
        }else if(response.contains("0")){
            return "off";
        }else if(response.contains("UP")){
            return "up";
        }else if(response.contains("DOWN")){
            return "down";
        }else if(response.contains("li")){
            return "on";
        }else{
            return "off";
        }
    }

    //Adapt default on/off status to the required status from telosb
    public String getStatus(String status){
        if(status.equals("on")){
            return "on";
        }else{
            return "off";
        }
    }

    public String getToggleStatus(String status){
        if(status.equals("on")){
            return "off";
        }else{
            return "on";
        }
    }

    public int getSengenStatus(String status){
        if(status.equals("on")){
            return 1;
        }else{
            return 0;
        }
    }

    public int getIcon(String status){
        if(status.equals("on")){
            return R.drawable.node_on;
        }else{
            return R.drawable.node_off;
        }
    }
}
