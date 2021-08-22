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

// ==================== 原生xhr调用 ====================

const xhr = new XMLHttpRequest()
xhr.open('GET', '/sse/custom/responseBodyEmitter', true)
//xhr.open('GET', '/sse/custom/streamingResponseBody', true)
//xhr.open('GET', '/sse/custom/sync', true)

// ========== 通过xhr的progress事件监听回调来处理自定义事件 ==========

//const responseStreamHandler = builder.buildForEvent();
//xhr.addEventListener('progress', responseStreamHandler)

// ========= 通过xhr的onreadystatechange回调处理自定义事件 ==========

const responseStreamHandler = builder.buildForXHR()
xhr.onreadystatechange = function() {
    if (xhr.readyState === 3) {
        responseStreamHandler(xhr)
    }
}
xhr.send()