package com.thirumalai.calllimiter.Fragment

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.thirumalai.calllimiter.R

class Intro : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_intro, container, false)
        val termsText: TextView = view.findViewById(R.id.terms_conditions)
        val text = "By continuing, you agree to our <a href='https://github.com/Thiru-Malai/CallLimiter/blob/master/TermsAndConditions.md'>Terms & Conditions</a>."
        termsText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        termsText.movementMethod = LinkMovementMethod.getInstance()
        return view
    }
}
