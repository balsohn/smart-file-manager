package com.smartfilemanager.model;

/**
 * 중복 파일 타입을 나타내는 열거형
 */
public enum DuplicateType {
    /**
     * 정확한 중복 - 파일 내용이 완전히 동일 (해시값 동일)
     */
    EXACT,

    /**
     * 유사한 파일 - 파일명이나 크기가 비슷하지만 내용은 다를 수 있음
     */
    SIMILAR
}