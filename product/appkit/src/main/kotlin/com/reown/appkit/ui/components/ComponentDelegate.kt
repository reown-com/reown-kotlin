package com.reown.appkit.ui.components

import com.reown.appkit.client.AppKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach

internal sealed class ComponentEvent(val isOpen: Boolean) {
    object ModalHiddenEvent : ComponentEvent(false)
    object ModalExpandedEvent : ComponentEvent(true)
}

internal object ComponentDelegate {

    val modalComponentEvent: MutableStateFlow<ComponentEvent> = MutableStateFlow(ComponentEvent.ModalHiddenEvent)

    var isModalOpen: Boolean = false

    fun setDelegate(delegate: AppKit.ComponentDelegate) {
        modalComponentEvent.onEach { event ->
            when (event) {
                ComponentEvent.ModalHiddenEvent -> delegate.onModalHidden()
                ComponentEvent.ModalExpandedEvent -> delegate.onModalExpanded()
            }
        }
    }

    fun openModalEvent() {
        modalComponentEvent.compareAndSet(ComponentEvent.ModalHiddenEvent, ComponentEvent.ModalExpandedEvent)
        isModalOpen = true
    }

    fun closeModalEvent() {
        modalComponentEvent.compareAndSet(ComponentEvent.ModalExpandedEvent, ComponentEvent.ModalHiddenEvent)
        isModalOpen = false
    }
}


