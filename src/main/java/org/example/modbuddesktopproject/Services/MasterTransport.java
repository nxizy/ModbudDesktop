package org.example.modbuddesktopproject.Services;

import org.example.modbuddesktopproject.Services.ENUMs.Protocol;

import java.io.IOException;

public interface MasterTransport {
    byte[] sendRequest(byte[] req) throws IOException, InterruptedException;
    Protocol whichProtocol();
}
