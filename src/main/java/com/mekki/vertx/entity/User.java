package com.mekki.vertx.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Maki on 2017/10/11.
 */
@Entity
@Table(name = "subscribe_users_t")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String openId;

    private Integer status; //1已关注 2未关注

    private String spcCardNum;

    private Date subscribeTime;

    private Date unsubscribeTime;

    public User() {
    }

    public User(String openId, Integer status) {
        this.openId = openId;
        this.status = status;
        subscribeTime = new Date();
    }

    public String getSpcCardNum() {
        return spcCardNum;
    }

    public void setSpcCardNum(String spcCardNum) {
        this.spcCardNum = spcCardNum;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getSubscribeTime() {
        return subscribeTime;
    }

    public void setSubscribeTime(Date subscribeTime) {
        this.subscribeTime = subscribeTime;
    }

    public Date getUnsubscribeTime() {
        return unsubscribeTime;
    }

    public void setUnsubscribeTime(Date unsubscribeTime) {
        this.unsubscribeTime = unsubscribeTime;
    }
}
