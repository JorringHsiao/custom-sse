
function ResponseStreamHandlerBuilder(params) {
    if (params) {
        this.shareEvent = params.shareEvent === false
    }
}

ResponseStreamHandlerBuilder.prototype = {
    eventPartSplitter: '\n',
    eventSplitter: '\n\n',
    shareEvent: true,
    globalListener: null,
    nameListeners: {},
    matcherListeners: [],
    setGlobalListener: function(listener) {
        this.globalListener = listener
        return this
    },
    addListener: function(matcher, listener) {
        if (typeof matcher === 'string') {
            this.nameListeners[matcher] = listener
        } else if (typeof matcher === 'function') {
            this.matcherListeners.push({matcher: matcher, listener: listener})
        }
        return this
    },
    matchListeners: function(eventName) {
        const collector = [];
        if (this.globalListener) {
            collector.push(this.globalListener)
        }
        if (this.nameListeners.hasOwnProperty(eventName)) {
            collector.push(this.nameListeners[eventName])
        }
        if (this.matcherListeners.length > 0) {
            for (let matcherListener of this.matcherListeners) {
                if (matcherListener.matcher(eventName)) {
                    collector.push(matcherListener.listener)
                }
            }
        }
        return collector
    },
    buildForEvent: function() {
        const context = {
            lastIndex: 0,
            builder: this
        }
        return function(e) {
            return context.builder._handleResponseStream(context, e.currentTarget)
        }
    },
    buildForXHR: function() {
        const context = {
            lastIndex: 0,
            builder: this
        }
        return function(xhr) {
            return context.builder._handleResponseStream(context, xhr)
        }
    },
    _handleResponseStream: function(context, xhr) {
        const builder = context.builder, responseText = xhr.responseText
        let nextIndex;
        while (true) {
            nextIndex = responseText.indexOf(builder.eventSplitter, context.lastIndex)
            if (nextIndex < 0) {
                break
            }
            const chunk = responseText.substring(context.lastIndex, nextIndex);
            context.lastIndex = nextIndex + builder.eventSplitter.length
            const parts = chunk.split(builder.eventPartSplitter);
            const listeners = builder.matchListeners(parts[0]);
            if (listeners.length > 0) {
                let curEvent = null;
                for (let listener of listeners) {
                    if (!curEvent || !builder.shareEvent) {
                        curEvent = {
                            name: parts[0],
                            data: parts[1] ? JSON.parse(parts[1]) : {}
                        }
                    }
                    listener(curEvent)
                }
            }
        }
    }
}