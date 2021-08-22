let eventCount = 100, eventCounter = 0, delayOrigin = 0, delayBound = 500;

function printProgress(curCount, count, message) {
    let p = ((curCount/count)*100).toFixed(2);
    $('#output').text('处理进度：' + p + '% (' + curCount + '/' + count +') ' + message)
}

printProgress(eventCounter, eventCount, '')

// 用封装好的工具创建响应流处理器
const builder = new ResponseStreamHandlerBuilder().setGlobalListener(e => {
    console.log(e)
    eventCounter++;
    printProgress(eventCounter, eventCount, e.data.message)
});

// ==================== axios调用 ====================

const url = '/demo/numProgress'
axios.get(url,{
    params: {
        // 模拟完成任务所需要的步骤数量
        count: eventCount,
        // 模拟每个“步骤”之间的延时随机范围：delayOrigin ~ delayBound
        delayOrigin: delayOrigin,
        delayBound: delayBound
    },
    onDownloadProgress: builder.buildForEvent()
}).then(function(res){
    setTimeout(() => alert('处理完成'), 100)
    console.log(res)
}).catch(function(res){
    console.log(res);
});