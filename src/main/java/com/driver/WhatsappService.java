package com.driver;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WhatsappService {

    WhatsappRepository whatsappRepository = new WhatsappRepository();

    public String createUser(String name, String mobile) throws Exception {

        try{
            if(whatsappRepository.getUserRepository().containsKey(mobile)){
                throw new Exception("User already exists");
            }else{
                User user = new User();
                user.setName(name);
                user.setMobile(mobile);
                whatsappRepository.getUserRepository().put(mobile,user);
            }
        }catch(Exception e){
            System.out.println(e);
        }
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) throws Exception {

        int noOfUsers = users.size();
        Group group = new Group();
        try{
            if(noOfUsers<2){
                throw  new Exception("Not enough users to create a group");
            }else if(noOfUsers==2){
                group.setName(users.get(1).getName());
                group.setNumberOfParticipants(noOfUsers);
                whatsappRepository.getGroupRepository().put(group,users);
            }else{
                int groupCount = 0;
                for(Group temp : whatsappRepository.getGroupRepository().keySet()){
                    if(temp.getNumberOfParticipants()>2){
                        groupCount++;
                    }
                }
                groupCount++;
                String groupName = "Group "+groupCount;
                group.setName(groupName);
                group.setNumberOfParticipants(noOfUsers);
                whatsappRepository.getGroupRepository().put(group,users);
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return group;
    }

    public int createMessage(String content) {

        int noOfMessages = whatsappRepository.getMessageRepository().size();
        Message message = new Message();
        message.setId(noOfMessages+1);
        message.setContent(content);
        message.setTimestamp(new Date());
        whatsappRepository.getMessageRepository().put(message.getId(),message);

        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {

        try{
            if(!whatsappRepository.getGroupRepository().containsKey(group)){
                throw  new Exception("Group does not exist");
            }
            boolean flag = false;
            for(User user  : whatsappRepository.getGroupRepository().get(group)){
                if(user.equals(sender)){
                    flag =true;
                }
            }
            if(flag ==false){
                throw  new Exception("You are not allowed to send message");
            }

            message.setGroup(group);
            message.setUser(sender);
            sender.getMessages().add(message);

            whatsappRepository.getUserRepository().put(sender.getName(),sender);

            group.getMessages().add(message);
            List<User> users = whatsappRepository.getGroupRepository().get(group);
            whatsappRepository.getGroupRepository().put(group,users);
        }catch (Exception e){
            System.out.println(e);
        }
        return group.getMessages().size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {

            if(!whatsappRepository.getGroupRepository().containsKey(group)){
            throw  new Exception("Group does not exist");
        }

        if(!whatsappRepository.getGroupRepository().get(group).get(0).equals(approver)){
            throw  new Exception("Approver does not have rights");
        }
        boolean flag = false;
        List<User> userList= whatsappRepository.getGroupRepository().get(group);
        int index =-1;
        for(User user1: userList){
            index++;
            if(user1.equals(user)){
                flag = true;
                break;
            }
        }
        if(flag==false){
            throw new Exception("User is not a participant");
        }

        User temp = userList.get(index);
        userList.set(index,userList.get(0));
        userList.set(0,temp);

        whatsappRepository.getGroupRepository().put(group,userList);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {

        boolean flag = false;
        int index=-1;
        Group userGroup = null;
        for(Group group: whatsappRepository.getGroupRepository().keySet()){
            for(User user1: whatsappRepository.getGroupRepository().get(group)){
                index++;
                if(user.equals(user1)){
                    userGroup = group;
                    flag = true;
                    break;
                }
            }
            if(flag==true){
                break;
            }
            index = -1;
        }

        if(flag==false){
            throw  new Exception("User not found");
        }
        if(index==0){
            throw new Exception("Cannot remove admin");
        }else{
            List<User> updatedUserList = new ArrayList<>();
            List<User> userLIst = whatsappRepository.getGroupRepository().get(userGroup);
            if(userLIst.isEmpty()){
                userLIst = new ArrayList<>();
            }
            else{
                for(User updatedUsers: userLIst){
                    if(updatedUsers.equals(user))
                        continue;
                    updatedUserList.add(updatedUsers);
                }
            }
            whatsappRepository.getGroupRepository().put(userGroup,updatedUserList);

            whatsappRepository.getUserRepository().remove(user.getName());

            userGroup.getMessages().removeIf(message -> message.getUser().equals(user));
            List<Message> messageList = userGroup.getMessages();
            List<Message> updatedMessageList = new ArrayList<>();
            for(Message updatedMessage : messageList){
                if(updatedMessage.getUser().equals(user) && updatedMessage.getGroup().equals(userGroup))
                    continue;
                updatedMessageList.add(updatedMessage);
            }
            userGroup.setMessages(updatedMessageList);
            whatsappRepository.getGroupRepository().put(userGroup,updatedUserList);

            for(int i: whatsappRepository.getMessageRepository().keySet()){
                if(whatsappRepository.getMessageRepository().get(i).getUser().equals(user) && whatsappRepository.getMessageRepository().get(i).getGroup().equals(userGroup)){
                    whatsappRepository.getMessageRepository().remove(i);
                }
            }
        }

        int noOfUsers = whatsappRepository.getGroupRepository().get(userGroup).size();
        int noOfMessagesInGroup = userGroup.getMessages().size();
        int overallMessages = whatsappRepository.getMessageRepository().size();
        return noOfUsers+noOfMessagesInGroup+ overallMessages;
    }

    public String findMessage(Date start, Date end, int k) throws Exception {

        List<Message> messageList = new ArrayList<>();
        for(Message message : whatsappRepository.getMessageRepository().values()){
            if(message.getTimestamp().compareTo(start)>0 && message.getTimestamp().compareTo(end)<0){
                messageList.add(message);
            }
        }
        Comparator<Message> compareByDate = (Message m1, Message m2) -> m1.getTimestamp().compareTo(m2.getTimestamp());

        Collections.sort(messageList,compareByDate);

        if(messageList.size()<k){
            throw new Exception("K is greater than the number of messages");
        }else{
            return messageList.get(k-1).getContent();
        }
    }
}
