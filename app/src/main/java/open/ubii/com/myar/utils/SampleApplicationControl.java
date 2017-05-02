/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package open.ubii.com.myar.utils;

import com.vuforia.State;


// 接口使用SampleApplicationSession实现的活动
public interface SampleApplicationControl
{
    
    // 被称为初始化跟踪器
    boolean doInitTrackers();
    
    
    // 被称为负载追踪器的数据
    boolean doLoadTrackersData();
    
    
    // 被称为开始跟踪和追踪者和他们的初始化
    // loaded data
    boolean doStartTrackers();
    
    
    // 停止追踪
    boolean doStopTrackers();
    
    
    // 摧毁追踪器的数据
    boolean doUnloadTrackersData();
    
    
    // To be deinitialize追踪器
    boolean doDeinitTrackers();
    
    
    // 这个回调被称为Vuforia初始化完成后
    // 追踪器初始化、数据加载和
    //跟踪已经准备好开始
    void onInitARDone(SampleApplicationException e);
    
    
    // This callback is called every cycle 回掉 一个循环
    void onVuforiaUpdate(State state);
    
}
