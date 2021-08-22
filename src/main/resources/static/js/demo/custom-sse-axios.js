// 用封装好的工具创建响应流处理器
const builder = new ResponseStreamHandlerBuilder().setGlobalListener(e => {
    output('listen all event: ' + JSON.stringify(e))
}).addListener('success', e => {
    output('listen success event by name:' + JSON.stringify(e))
}).addListener('error', e => {
    output('listen error event by name:' + JSON.stringify(e))
}).addListener('xxx', e => {
    output('listen xxx event by name:' + JSON.stringify(e))
}).addListener(name => name === 'success', e => {
    output('listen success event by matcher:' + JSON.stringify(e))
}).addListener(name => name.startsWith('err'), e => {
    output('listen error event by matcher:' + JSON.stringify(e))
});

// ==================== axios调用 ====================

//const url = '/sse/custom/responseBodyEmitter'
//const url = '/sse/custom/streamingResponseBody'
const url = '/sse/custom/sync'
axios.get(url,{
    // axios适配xhr时，onDownloadProgress回调函数会被设置到xhr的progress事件
    onDownloadProgress: builder.buildForEvent()
}).then(function(res){
    // 业务逻辑都在listener中处理，这里仅是请求结束的标志
    console.log(res)
}).catch(function(res){
    console.log(res);
});