package com.lojapp.service;

public interface NfeRawXmlStorage {

    record StoredRawXml(String rawXml, String rawXmlKey) {}

    StoredRawXml persist(long userId, String rawXml);

    String retrieve(String rawXml, String rawXmlKey);
}
