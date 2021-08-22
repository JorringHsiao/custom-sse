// 缺点：
// 1. 仅支持GET请求，无法传比较大的参数，也不能上传文件
// 2. 不能设置请求头
// 3. 会一直循环发起请求，没有onclose的回调方法，只能手动调close方法停止；
//    假如服务端异常没处理好，没有发送到"close"消息，那么前端将一直请求
const eventSource = new EventSource('/sse/standard')
let loopCount = 0
eventSource.onopen = function() {
    loopCount++
    output('==================== loop count：' + loopCount + ' ====================')
}
eventSource.onmessage = function(e) {
    console.log(e)
    output('EventSource.onmessage: ' + e.data)
}
//eventSource.close()