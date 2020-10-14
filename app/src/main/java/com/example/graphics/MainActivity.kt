package com.example.graphics

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import jxl.Sheet
import jxl.Workbook
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    //Переменные для данных графа
    private lateinit var fpg: LineGraphSeries<DataPoint>
    private lateinit var red: LineGraphSeries<DataPoint>
    private lateinit var nonFilter: LineGraphSeries<DataPoint>
    private lateinit var filtered: LineGraphSeries<DataPoint>
    private lateinit var heartRate: LineGraphSeries<DataPoint>
    private lateinit var spO2: LineGraphSeries<DataPoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //инициализация и добавление данных к графу
        fpg = LineGraphSeries<DataPoint>()
        graph.addSeries(fpg)

        red = LineGraphSeries<DataPoint>()
        graphRed.addSeries(red)

        nonFilter = LineGraphSeries<DataPoint>()
        graphNonFilter.addSeries(nonFilter)

        filtered = LineGraphSeries<DataPoint>()
        graphFiltered.addSeries(filtered)

        heartRate = LineGraphSeries<DataPoint>()
        graphHeartRate.addSeries(heartRate)

        spO2 = LineGraphSeries<DataPoint>()
        graphSpO2.addSeries(spO2)

    }

    override fun onResume() {
        super.onResume()
        //открываем xls файл
        var input = assets.open("test.xls")

        var wb = Workbook.getWorkbook(input)
        //берем первую страницу с экселя
        var sheet = wb.getSheet(0)
        //получаем количество строк
        var row = sheet.rows
        //массив пиков для ЧСС
        var peaks: MutableList<Int> = mutableListOf()

        //Инфракрасный ФПГ
        drawGraph(sheet, 0, row, graph,fpg)

        //Красный ФПГ
        drawGraph(sheet, 1, row, graphRed,red)

        //Нефильтрованный ЭКГ
        drawGraph(sheet, 2, row, graphNonFilter,nonFilter)

        //Фильтрованный ЭКГ
        Thread(Runnable {
            for (i in 0 until row) {
                val prevPoint = if (i == 0) {
                    sheet.getCell(3, i).contents.toDouble() - 1
                } else {
                    sheet.getCell(3, i - 1).contents.toDouble()
                }

                val point = sheet.getCell(3, i).contents.toDouble()
                val nextPoint = if (i < row - 1) {
                    sheet.getCell(3, i + 1).contents.toDouble()
                } else {
                    sheet.getCell(3, i).contents.toDouble() - 1
                }
                if (point > nextPoint && point > prevPoint) {
                    peaks.add(i)
                }
            }
            var j: Int = 0
            for (i in 0 until row) {
                runOnUiThread {
                    if (i % 2 == 0){
                    val point = sheet.getCell(3, i).contents.toDouble()
                    val viewportFiltered = graphFiltered.viewport
                    viewportFiltered.isXAxisBoundsManual = true
                    viewportFiltered.isScrollable = true
                    viewportFiltered.setMinX(i.toDouble())
                    viewportFiltered.setMaxX(i.toDouble()+ 700)
                    viewportFiltered.setMinY(-30000.0)
                    viewportFiltered.setMaxY(30000.0)
                    filtered.appendData(
                        DataPoint(
                            i.toDouble(),
                            point
                        ), true, 350
                    )}

                    //вывод пиков
                    if (i == peaks[j]) {
                        if (j != peaks.size - 1) {
                            Log.e("Here", "click")
                            val viewportHeartRate = graphHeartRate.viewport
                            viewportHeartRate.isXAxisBoundsManual = true
                            viewportHeartRate.isScrollable = true
                            viewportHeartRate.setMinX(i.toDouble() - 50)
                            viewportHeartRate.setMaxX(i.toDouble())
                            viewportHeartRate.setMinY(peaks[j].toDouble() - 100)
                            viewportHeartRate.setMaxY(peaks[j].toDouble() + 100)
                            heartRate.appendData(
                                DataPoint(
                                    peaks[j + 1].toDouble(),
                                    ((60 * 200) / (peaks[j + 1] - peaks[j])).toDouble()
                                ), true, 350
                            )
                            j++
                        }
                    }
                }
                try {
                    Thread.sleep(7)
                } catch (e: InterruptedException) {
                }
            }
        }).start()

        //SpO2
        Thread(Runnable {
            for (i in 0 until row) {
                runOnUiThread {
                    val viewportSpO2 = graphSpO2.viewport
                    viewportSpO2.isXAxisBoundsManual = true
                    viewportSpO2.isScrollable = true
                    viewportSpO2.setMinX(i.toDouble() - 50)
                    viewportSpO2.setMaxX(i.toDouble())
                    viewportSpO2.setMinY(sheet.getCell(2, i).contents.toDouble() - 5000)
                    viewportSpO2.setMaxY(sheet.getCell(2, i).contents.toDouble() + 5000)
                    spO2.appendData(
                        DataPoint(
                            i.toDouble(),
                            sheet.getCell(2, i).contents.toDouble()
                        ), true, 200
                    )
                }
                // sleep to slow down the add of entries
                try {
                    Thread.sleep(14)
                } catch (e: InterruptedException) { // manage error ...
                }
            }
        }).start()
    }

    //Функция отрисовки графа
    private fun drawGraph(
        sheet: Sheet,
        column: Int,
        row: Int,
        graph: GraphView,
        series: LineGraphSeries<DataPoint>
    ) {
        //Создаем новый поток
        Thread(Runnable {
            //запускаем цикл чтобы пробежать по всем строкам
            for (i in 0 until row) {
                runOnUiThread {
                    //настройки для графа
                    if (i % 2 == 0){
                    val viewport = graph.viewport
                    viewport.isXAxisBoundsManual = true
                    viewport.isScrollable = true
                    //минимальное и максимальное значение по осям
                    viewport.setMinX(i.toDouble())
                    viewport.setMaxX(i.toDouble()+ 700)
                    viewport.setMinY(sheet.getCell(column, i).contents.toDouble() - 5000)
                    viewport.setMaxY(sheet.getCell(column, i).contents.toDouble() + 5000)
                    //добавляем новые данные по мере поступления

                        series.appendData(
                        DataPoint(
                            i.toDouble(),
                            sheet.getCell(column, i).contents.toDouble()
                        ), true, 350
                    )}
                }

                try {
                    //задержка перед новыми данными
                    Thread.sleep(7)
                } catch (e: InterruptedException) {
                }
            }
        }).start() //запуск потока
    }
}
