package com.sam_chordas.android.stockhawk.entity;

/**
 * Created by ByungHwa on 4/5/2016.
 */
public class Stock {

    private int _id;
    private String dateString;
    private String bidPrice;

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(String bidPrice) {
        this.bidPrice = bidPrice;
    }
}
