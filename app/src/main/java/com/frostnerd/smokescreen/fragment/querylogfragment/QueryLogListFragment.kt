package com.frostnerd.smokescreen.fragment.querylogfragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.frostnerd.cacheadapter.ModelAdapterBuilder
import com.frostnerd.smokescreen.R
import com.frostnerd.smokescreen.database.entities.DnsQuery
import com.frostnerd.smokescreen.database.getDatabase
import com.frostnerd.smokescreen.fragment.QueryLogFragment
import com.frostnerd.smokescreen.util.LiveDataSource
import kotlinx.android.synthetic.main.fragment_querylog_list.*
import java.text.DateFormat
import java.util.*

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
class QueryLogListFragment: Fragment() {
    companion object {
        internal lateinit var timeFormatSameDay: DateFormat
        internal lateinit var timeFormatDifferentDay: DateFormat
        internal fun formatTimeStamp(timestamp:Long): String {
            return if(isTimeStampToday(timestamp)) timeFormatSameDay.format(timestamp)
            else timeFormatDifferentDay.format(timestamp)
        }

        private fun isTimeStampToday(timestamp:Long):Boolean {
            return timestamp >= getStartOfDay()
        }

        private fun getStartOfDay():Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupTimeFormat()
        return layoutInflater.inflate(R.layout.fragment_querylog_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val live = requireContext().getDatabase().dnsQueryDao().getAllLive()
        val source = LiveDataSource(this, live, true) {
            list.smoothScrollToPosition(0)
        }
        val adapter = ModelAdapterBuilder.newBuilder(source) {
            viewBuilder = { parent, viewType ->
                val createdView = layoutInflater.inflate(R.layout.item_logged_query, parent, false)
                createdView.setOnClickListener {
                    displayQuery(it.tag as DnsQuery)
                }
                createdView
            }
            getItemCount = {
                source.currentSize()
            }
            bindModelView = { viewHolder, position, data ->
                viewHolder.itemView.findViewById<TextView>(R.id.text).text = data.shortName
                viewHolder.itemView.findViewById<TextView>(R.id.time).text = formatTimeStamp(data.questionTime)
                viewHolder.itemView.tag = data
                if(isDisplayingQuery(data)) displayQuery(data, false)
            }
            bindNonModelView = { viewHolder, position ->

            }
            runOnUiThread = {
                requireActivity().runOnUiThread(it)
            }
        }.build()

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
    }

    private fun setupTimeFormat() {
        val locale = getLocale()
        timeFormatSameDay = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
        timeFormatDifferentDay = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)
    }

    private fun getLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            resources.configuration.locales.get(0)!!
        } else{
            @Suppress("DEPRECATION")
            resources.configuration.locale!!
        }
    }

    private fun displayQuery(dnsQuery: DnsQuery, switchToDetailView:Boolean = true) {
        (parentFragment as QueryLogFragment).displayQueryDetailed(dnsQuery, switchToDetailView)
    }

    private fun isDisplayingQuery(dnsQuery: DnsQuery):Boolean {
        val parent = parentFragment as QueryLogFragment
        if(!parent.detailFragment.isShowingQuery()) return false
        return parent.detailFragment.currentQuery?.id == dnsQuery.id
    }
}