package com.ekc.recorder.util;

public record XmlChange(String xpath, XmlChangeType type, String beforeFragment, String afterFragment) {
}
