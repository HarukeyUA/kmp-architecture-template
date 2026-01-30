package org.example.project.core.component.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle as AndroidLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.subscribe

/**
 * Bridges Essenty's Lifecycle to AndroidX LifecycleOwner.
 * This allows Compose lifecycle-aware APIs to work with Decompose components.
 */
class EssentyLifecycleOwner(private val essentyLifecycle: Lifecycle) : LifecycleOwner {
    override val lifecycle: AndroidLifecycle = object : AndroidLifecycle() {
        private val observers = mutableMapOf<LifecycleObserver, Lifecycle.Callbacks>()

        override val currentState: State
            get() = when (essentyLifecycle.state) {
                Lifecycle.State.DESTROYED -> State.DESTROYED
                Lifecycle.State.INITIALIZED -> State.INITIALIZED
                Lifecycle.State.CREATED -> State.CREATED
                Lifecycle.State.STARTED -> State.STARTED
                Lifecycle.State.RESUMED -> State.RESUMED
            }

        override fun addObserver(observer: LifecycleObserver) {
            if (observer is DefaultLifecycleObserver) {
                val callbacks = object : Lifecycle.Callbacks {
                    override fun onCreate() {
                        observer.onCreate(this@EssentyLifecycleOwner)
                    }

                    override fun onDestroy() {
                        observer.onDestroy(this@EssentyLifecycleOwner)
                    }

                    override fun onPause() {
                        observer.onPause(this@EssentyLifecycleOwner)
                    }

                    override fun onResume() {
                        observer.onResume(this@EssentyLifecycleOwner)
                    }

                    override fun onStart() {
                        observer.onStart(this@EssentyLifecycleOwner)
                    }

                    override fun onStop() {
                        observer.onStop(this@EssentyLifecycleOwner)
                    }
                }

                observers[observer] = callbacks
                essentyLifecycle.subscribe(callbacks = callbacks)
            } else if (observer is LifecycleEventObserver) {
                val callbacks = object : Lifecycle.Callbacks {
                    override fun onCreate() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_CREATE
                        )
                    }

                    override fun onStart() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_START
                        )
                    }

                    override fun onResume() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_RESUME
                        )
                    }

                    override fun onPause() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_PAUSE
                        )
                    }

                    override fun onStop() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_STOP
                        )
                    }

                    override fun onDestroy() {
                        observer.onStateChanged(
                            source = this@EssentyLifecycleOwner,
                            event = Event.ON_DESTROY
                        )
                    }
                }

                observers[observer] = callbacks
                essentyLifecycle.subscribe(callbacks)
            }
        }

        override fun removeObserver(observer: LifecycleObserver) {
            observers.remove(observer)?.also { callbacks ->
                essentyLifecycle.unsubscribe(callbacks)
            }
        }
    }
}
