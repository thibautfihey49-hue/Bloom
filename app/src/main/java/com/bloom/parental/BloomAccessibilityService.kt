package com.bloom.parental
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
class BloomAccessibilityService:AccessibilityService(){
    override fun onAccessibilityEvent(e:AccessibilityEvent?){}
    override fun onInterrupt(){}
}
