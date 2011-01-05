package org.smartfrog.services.anubis.partition.wire.security;

import java.io.IOException;

import javax.crypto.ShortBufferException;

import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

public class MACSecurityImpl implements WireSecurity {

    private MACData macData;

    public WireMsg fromWireForm(byte[] wireForm) throws WireSecurityException,
                                                WireFormException {
        try {

            WireMsg msg = Wire.fromWire(wireForm);
            macData.checkMAC(wireForm, 0, msg.getSize() - 1);

            return msg;
        } catch (ClassNotFoundException e) {
            throw new WireFormException("Unable to unmarshall message", e);
        } catch (IOException e) {
            throw new WireFormException("Unable to unmarshall message", e);
        } catch (SecurityException e) {
            throw new WireSecurityException(
                                            "Security violation unmarshalling message",
                                            e);
        }
    }

    public MACData getMacData() {
        return macData;
    }

    public void setMacData(MACData macData) {
        this.macData = macData;
    }

    public byte[] toWireForm(WireMsg msg) throws WireFormException {
        try {

            msg.setTrailerSize(macData.getMacSize());
            //            msg.setTrailerSize(100);
            byte[] wireForm = msg.toWire();
            macData.addMAC(wireForm, 0, msg.getSize() - 1);
            return wireForm;

        } catch (IOException e) {
            throw new WireFormException("Unable to marshall message", e);
        } catch (ShortBufferException e) {
            throw new WireFormException(
                                        "Unable to marshall message - buffer not large enough for MAC security data",
                                        e);
        } catch (SecurityException e) {
            throw new WireFormException(
                                        "Unable to marshall message - security issue",
                                        e);
        }
    }

}
