package com.sensacare.app

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * RingDetailActivity - Temporarily Stubbed
 * 
 * This is a temporary stub implementation that removes all VeePoo SDK dependencies.
 * It simply displays a "Coming Soon" message.
 * 
 * The full implementation will be added once the core connection functionality
 * is working properly.
 */
class RingDetailActivity : AppCompatActivity() {
    
    private lateinit var messageTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // First try to load the original layout
        setContentView(R.layout.activity_ring_detail)
        
        // Create a generic message TextView
        setupMessageDisplay()
        
        // Hide any other views that might be in the layout
        hideOtherViews()
    }
    
    private fun setupMessageDisplay() {
        // Try to find any TextView in the layout to reuse
        val rootView = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup
        var existingTextView: TextView? = null
        
        // Search for any TextView in the layout
        if (rootView != null) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                if (child is TextView) {
                    existingTextView = child
                    break
                } else if (child is ViewGroup) {
                    // Search one level deeper
                    for (j in 0 until child.childCount) {
                        val innerChild = child.getChildAt(j)
                        if (innerChild is TextView) {
                            existingTextView = innerChild
                            break
                        }
                    }
                }
            }
        }
        
        // Use existing TextView or create a new one
        if (existingTextView != null) {
            messageTextView = existingTextView
        } else {
            // Create a new TextView and set it as the content view
            messageTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                text = "Ring Device Details - Coming Soon"
                textSize = 18f
                setPadding(32, 64, 32, 64)
                gravity = Gravity.CENTER
            }
            
            // Create a container and add the TextView
            val container = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                addView(messageTextView)
            }
            
            setContentView(container)
        }
        
        // Set the message text
        messageTextView.text = "Ring Device Details - Coming Soon\n\n" +
                "This screen will display details about your connected ring device including:\n" +
                "- Battery Level\n" +
                "- Device Information\n" +
                "- Firmware Version\n" +
                "- Connection Status"
    }
    
    private fun hideOtherViews() {
        // Find the root view
        val rootView = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup
        
        // Hide all views except our message TextView
        if (rootView != null) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                if (child != messageTextView && child !is androidx.appcompat.widget.Toolbar) {
                    try {
                        child.visibility = android.view.View.GONE
                    } catch (e: Exception) {
                        // Ignore any errors when trying to hide views
                    }
                }
            }
        }
    }
}
