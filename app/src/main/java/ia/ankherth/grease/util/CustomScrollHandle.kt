package ia.ankherth.grease.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.ScrollHandle
import ia.ankherth.grease.R

class CustomScrollHandle(private val context: Context) : RelativeLayout(context), ScrollHandle {

    private var pdfView: PDFView? = null
    private var textView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }
    private var currentPosition = 0f
    private var shown = false
    private var isDragging = false
    private var initialY = 0f
    private var initialTouchY = 0f

    init {
        visibility = INVISIBLE
        alpha = 0f
    }

    override fun setupLayout(pdfView: PDFView) {
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        params.addRule(ALIGN_PARENT_END)
        params.addRule(ALIGN_PARENT_TOP)
        params.setMargins(0, 0, 16, 0) // Margen derecho

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_scroll_handle, this, true)
        textView = view.findViewById(R.id.scrollHandleText)

        pdfView.addView(this, params)
        this.pdfView = pdfView

        setOnClickListener { /* No hacer nada al hacer clic */ }
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        if (!shown) {
            show()
        } else {
            handler.removeCallbacks(hideRunnable)
        }

        currentPosition = position
        updatePosition(position)
        updateText()
        handler.postDelayed(hideRunnable, 1000) // Ocultar después de 1 segundo
    }

    private fun updatePosition(position: Float) {
        pdfView?.let { pdf ->
            // Calcular la posición Y basada en el progreso del PDF
            val parentHeight = (parent as? ViewGroup)?.height ?: pdf.height
            val handleHeight = height

            // position va de 0f a 1f, donde 0 es el inicio y 1 es el final
            val maxY = parentHeight - handleHeight
            val newY = (maxY * position).coerceIn(0f, maxY.toFloat())

            // Animar suavemente el movimiento
            animate()
                .translationY(newY)
                .setDuration(100)
                .start()
        }
    }

    private fun updateText() {
        pdfView?.let { pdf ->
            val currentPage = pdf.currentPage + 1
            textView?.text = "$currentPage"
        }
    }

    override fun hideDelayed() {
        handler.postDelayed(hideRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        textView?.text = pageNum.toString()
    }

    override fun shown(): Boolean = shown

    override fun show() {
        if (!shown) {
            shown = true
            visibility = VISIBLE
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    override fun hide() {
        if (shown) {
            shown = false
            animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    visibility = INVISIBLE
                }
                .start()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Guardar posición inicial
                isDragging = true
                initialY = translationY
                initialTouchY = event.rawY

                // Cancelar el auto-hide mientras se arrastra
                handler.removeCallbacks(hideRunnable)

                // Asegurar que esté visible
                if (!shown) {
                    show()
                }

                // Animación elegante al tocar: agrandar y aumentar elevación
                textView?.animate()
                    ?.scaleX(1.3f)
                    ?.scaleY(1.3f)
                    ?.translationZ(12f)
                    ?.setDuration(150)
                    ?.start()

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    pdfView?.let { pdf ->
                        val parentHeight = (parent as? ViewGroup)?.height ?: pdf.height
                        val handleHeight = height
                        val maxY = parentHeight - handleHeight

                        // Calcular nueva posición Y
                        val deltaY = event.rawY - initialTouchY
                        val newY = (initialY + deltaY).coerceIn(0f, maxY.toFloat())

                        // Actualizar posición del handle con animación suave
                        animate()
                            .translationY(newY)
                            .setDuration(0)
                            .start()

                        // Calcular y aplicar la nueva posición del PDF
                        val position = if (maxY > 0) newY / maxY else 0f
                        currentPosition = position

                        // Mover el PDF a la posición correspondiente
                        val targetPage = (position * (pdf.pageCount - 1)).toInt().coerceIn(0, pdf.pageCount - 1)
                        pdf.jumpTo(targetPage, false)

                        // Actualizar texto
                        updateText()
                    }

                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false

                    // Animación elegante al soltar: volver al tamaño normal con efecto de rebote
                    textView?.animate()
                        ?.scaleX(1.0f)
                        ?.scaleY(1.0f)
                        ?.translationZ(6f)
                        ?.setDuration(200)
                        ?.setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                        ?.start()

                    // Programar auto-hide después de soltar
                    handler.postDelayed(hideRunnable, 1000)
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }
}
