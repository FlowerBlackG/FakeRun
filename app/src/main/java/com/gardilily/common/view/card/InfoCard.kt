/*
 * Info Card
 * A convenient info card as part of the Gardilily Android Development Tools.
 *
 * Author : Flower Black
 * Version: 2021.07.05-13:06
 */

package com.gardilily.common.view.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.gardilily.common.view.card.InfoCard.Builder

/**
 * 用于显示基本信息的卡片。继承自 RelativeLayout
 *
 * 卡片分为左、右、顶、中四个部分，分别为：
 *
 * · 标题（顶部靠左
 * · 图标（左侧方块，若为RTL布局则在右侧）
 * · 小标签（右侧方块，若为RTL布局则在左侧）
 * · 信息列表（中部）
 *
 * 其中，信息列表为多行布局。每行有小标题和内容，之间加入分隔符（默认为中文冒号"："）。
 *
 * 卡片整体样式如下：
 *
 *     张三
 * 🍓  学校：同济大学
 *     住址：上海市杨浦区
 *     收货：上海市杨浦区四平路
 *          1239号同济大学      A（标签）
 *     编号：001
 *
 * 使用 [Builder.build()][Builder.build] 构造对象。
 */
open class InfoCard internal constructor(
	builder: Builder
) : RelativeLayout(builder.c) {

	constructor(context: Context) : this(Builder(context))

	val c = builder.c
	val spMultiply = builder.spMultiply
	val cardBackground: Drawable? = builder.cardBackground
	val outerMarginBottomSp = builder.outerMarginBottomSp
	val outerMarginTopSp = builder.outerMarginTopSp
	val outerMarginStartSp = builder.outerMarginStartSp
	val outerMarginEndSp = builder.outerMarginEndSp
	val innerMarginBetweenSp = builder.innerMarginBetweenSp
	val innerMarginTopSp = builder.innerMarginTopSp
	val innerMarginBottomSp = builder.innerMarginBottomSp
	val innerMarginStartSp = builder.innerMarginStartSp
	val innerMarginEndSp = builder.innerMarginEndSp
	val textLineSpaceSp = builder.textLineSpaceSp
	val layoutWidth = builder.layoutWidth
	val layoutHeight = builder.layoutHeight
	val hasIcon = builder.hasIcon
	val icon = builder.icon
	val iconTextSizeSp = builder.iconTextSizeSp
	val hasEndMark = builder.hasEndMark
	val endMark = builder.endMark
	val endMarkTextSizeSp = builder.endMarkTextSizeSp
	val endMarkMarginEndSp = builder.endMarkMarginEndSp
	val endMarkMarginBottomSp = builder.endMarkMarginBottomSp
	val title = builder.title
	val titleTextSizeSp = builder.titleTextSizeSp
	val titleMaxEms = builder.titleMaxEms
	val titleMaxLines = builder.titleMaxLines
	val titleEllipsize = builder.titleEllipsize
	val infoTextSizeSp = builder.infoTextSizeSp
	val infoList = builder.infoList

	init {
		val params = LayoutParams(
			layoutWidth,
			layoutHeight
		)
		params.marginStart = floatSp2intPx(outerMarginStartSp)
		params.marginEnd = floatSp2intPx(outerMarginEndSp)
		params.topMargin = floatSp2intPx(outerMarginTopSp)
		params.bottomMargin = floatSp2intPx(outerMarginBottomSp)
		this.layoutParams = params

		if (cardBackground != null) {
			this.background = cardBackground
		}

		this.isClickable = true

		val iconView = TextView(c)
		iconView.text = icon
		iconView.textSize = iconTextSizeSp
		iconView.setTextColor(Color.BLACK)

		iconView.visibility =
			if (hasIcon) {
				View.VISIBLE
			} else {
				View.GONE
			}

		val iconViewParams = LayoutParams(
			LayoutParams.WRAP_CONTENT,
			LayoutParams.WRAP_CONTENT
		)
		iconViewParams.marginStart = floatSp2intPx(innerMarginStartSp)
		iconViewParams.addRule(CENTER_VERTICAL)

		iconView.layoutParams = iconViewParams

		this.addView(iconView)

		val endMarkView = TextView(c)
		endMarkView.text = endMark
		endMarkView.textSize = endMarkTextSizeSp
		endMarkView.visibility =
			if (hasEndMark) {
				View.VISIBLE
			} else {
				View.GONE
			}
		val endMarkViewParams = LayoutParams(
			LayoutParams.WRAP_CONTENT,
			LayoutParams.WRAP_CONTENT
		)
		endMarkViewParams.marginEnd = floatSp2intPx(endMarkMarginEndSp)
		endMarkViewParams.bottomMargin = floatSp2intPx(endMarkMarginBottomSp)
		endMarkViewParams.addRule(ALIGN_PARENT_END)
		endMarkViewParams.addRule(ALIGN_PARENT_BOTTOM)
		endMarkView.layoutParams = endMarkViewParams

		this.addView(endMarkView)

		val infoLinearLayout = LinearLayout(c)
		infoLinearLayout.orientation = LinearLayout.VERTICAL
		infoLinearLayout.gravity = Gravity.CENTER_VERTICAL

		val infoLinearLayoutParams = LayoutParams(
			LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT
		)
		infoLinearLayoutParams.marginStart = floatSp2intPx(
			innerMarginStartSp +
					if (hasIcon) {
						innerMarginBetweenSp
					} else {
						0f
					}
		) + iconView.paint.measureText(icon).toInt()

		infoLinearLayoutParams.marginEnd = floatSp2intPx(innerMarginEndSp)
		infoLinearLayoutParams.topMargin = floatSp2intPx(innerMarginTopSp)
		infoLinearLayoutParams.bottomMargin = floatSp2intPx(innerMarginBottomSp)

		infoLinearLayout.layoutParams = infoLinearLayoutParams

		val titleTV = TextView(c)
		titleTV.text = title
		titleTV.textSize = titleTextSizeSp
		titleTV.maxEms = titleMaxEms
		titleTV.maxLines = titleMaxLines
		titleTV.ellipsize = titleEllipsize

		titleTV.setTextColor(Color.WHITE)

		infoLinearLayout.addView(titleTV)

		val endMarkTVLen = endMarkView.paint.measureText(endMark).toInt()

		infoList.forEach {
			val row = LinearLayout(c)
			row.orientation = LinearLayout.HORIZONTAL
			val rowParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			if (hasEndMark) {
				rowParams.marginEnd = (floatSp2intPx(
					innerMarginBetweenSp
							+ endMarkMarginEndSp
							- innerMarginEndSp
				) + endMarkTVLen).coerceAtLeast(0)
			}
			rowParams.topMargin = floatSp2intPx(textLineSpaceSp)
			row.layoutParams = rowParams

			val tvParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			val tvTitle = TextView(c)
			tvTitle.layoutParams = tvParams
			tvTitle.textSize = infoTextSizeSp
			tvTitle.text = "${it.title}："
			tvTitle.setTextColor(Color.WHITE)
			val tvText = TextView(c)
			tvText.layoutParams = tvParams
			tvText.textSize = infoTextSizeSp
			tvText.text = it.text
			tvText.setTextColor(Color.WHITE)

			row.addView(tvTitle)
			row.addView(tvText)

			infoLinearLayout.addView(row)
		}

		this.addView(infoLinearLayout)
	}

	class Builder constructor(context: Context) {
		val c = context

		/**
		 * 将 Sp 单位转换为 像素 单位，需要乘以的常数。
		 */
		var spMultiply = 1f
		fun setSpMultiply(spMultiply: Float) = apply {
			this.spMultiply = spMultiply
		}

		/**
		 * 卡片背景。
		 */
		var cardBackground: Drawable? = null
		fun setCardBackground(cardBackground: Drawable?) = apply {
			this.cardBackground = cardBackground
		}

		var outerMarginBottomSp = 12f
		fun setOuterMarginBottomSp(outerMarginBottomSp: Float) = apply {
			this.outerMarginBottomSp = outerMarginBottomSp
		}

		var outerMarginTopSp = 0f
		fun setOuterMarginTopSp(outerMarginTopSp: Float) = apply {
			this.outerMarginTopSp = outerMarginTopSp
		}

		var outerMarginStartSp = 0f
		fun setOuterMarginStartSp(outerMarginStartSp: Float) = apply {
			this.outerMarginStartSp = outerMarginStartSp
		}

		var outerMarginEndSp = 0f
		fun setOuterMarginEndSp(outerMarginEndSp: Float) = apply {
			this.outerMarginEndSp = outerMarginEndSp
		}

		var innerMarginBetweenSp = 12f
		fun setInnerMarginBetweenSp(innerMarginBetweenSp: Float) = apply {
			this.innerMarginBetweenSp = innerMarginBetweenSp
		}

		var innerMarginTopSp = 12f
		fun setInnerMarginTopSp(innerMarginTopSp: Float) = apply {
			this.innerMarginTopSp = innerMarginTopSp
		}

		var innerMarginBottomSp = 12f
		fun setInnerMarginBottomSp(innerMarginBottomSp: Float) = apply {
			this.innerMarginBottomSp = innerMarginBottomSp
		}

		var innerMarginStartSp = 12f
		fun setInnerMarginStartSp(innerMarginStartSp: Float) = apply {
			this.innerMarginStartSp = innerMarginStartSp
		}

		var innerMarginEndSp = 12f
		fun setInnerMarginEndSp(innerMarginEndSp: Float) = apply {
			this.innerMarginEndSp = innerMarginEndSp
		}

		var textLineSpaceSp = 1f
		fun setTextLineSpaceSp(textLineSpaceSp: Float) = apply {
			this.textLineSpaceSp = textLineSpaceSp
		}

		var layoutWidth = LayoutParams.MATCH_PARENT
		fun setLayoutWidth(layoutWidth: Int) = apply {
			this.layoutWidth = layoutWidth
		}

		var layoutHeight = LayoutParams.WRAP_CONTENT
		fun setLayoutHeight(layoutHeight: Int) = apply {
			this.layoutHeight = layoutHeight
		}
		fun setLayoutHeightSp(layoutHeightSp: Float) = apply {
			this.layoutHeight = (layoutHeightSp * spMultiply).toInt()
		}

		var hasIcon = true
		fun setHasIcon(hasIcon: Boolean) = apply {
			this.hasIcon = hasIcon
		}

		var icon = "🍓"
		fun setIcon(icon: String) = apply {
			this.icon = icon
		}

		var iconTextSizeSp = 48f
		fun setIconTextSizeSp(iconTextSizeSp: Float) = apply {
			this.iconTextSizeSp = iconTextSizeSp
		}

		var hasEndMark = false
		fun setHasEndMark(hasEndMark: Boolean) = apply {
			this.hasEndMark = hasEndMark
		}

		var endMark = "A"
		fun setEndMark(endMark: String) = apply {
			this.endMark = endMark
		}

		var endMarkTextSizeSp = 52f
		fun setEndMarkTextSizeSp(endMarkTextSizeSp: Float) = apply {
			this.endMarkTextSizeSp = endMarkTextSizeSp
		}

		var endMarkMarginEndSp = 24f
		fun setEndMarkMarginEndSp(endMarkMarginEndSp: Float) = apply {
			this.endMarkMarginEndSp = endMarkMarginEndSp
		}

		var endMarkMarginBottomSp = 18f
		fun setEndMarkMarginBottomSp(endMarkMarginBottomSp: Float) = apply {
			this.endMarkMarginBottomSp = endMarkMarginBottomSp
		}

		var title = "标题"
		fun setTitle(title: String) = apply {
			this.title = title
		}

		var titleTextSizeSp = 24f
		fun setTitleTextSizeSp(titleTextSizeSp: Float) = apply {
			this.titleTextSizeSp = titleTextSizeSp
		}

		fun setTitleMaxEms(titleMaxEms: Int) = apply {
			this.titleMaxEms = titleMaxEms
		}

		var titleMaxEms = 12

		fun setTitleMaxLines(titleMaxLines: Int) = apply {
			this.titleMaxLines = titleMaxLines
		}

		var titleMaxLines = 1

		fun setTitleEllipsize(titleEllipsize: TextUtils.TruncateAt) = apply {
			this.titleEllipsize = titleEllipsize
		}

		var titleEllipsize = TextUtils.TruncateAt.END

		fun setInfoTextSizeSp(infoTextSizeSp: Float) = apply {
			this.infoTextSizeSp = infoTextSizeSp
		}

		var infoTextSizeSp = 14f

		val infoList = ArrayList<Info>()

		fun addInfo(info: Info) = apply {
			this.infoList.add(info)
		}

		fun build(): InfoCard = InfoCard(this)
	}

	data class Info(val title: String, val text: String, val divider: String = "：")

	/**
	 * 将 Sp 转换为 Px 单位。并将原浮点数变为整数。
	 *
	 * @param value - 以 sp 为单位的长度
	 * @return 以 px 为单位的长度
	 */
	private fun floatSp2intPx(value: Float): Int {
		return (value * spMultiply).toInt()
	}
}
