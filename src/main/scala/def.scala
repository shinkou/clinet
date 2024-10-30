package com.shinkou.clinet

import java.awt.{Color,Dimension,Insets,Toolkit}
import java.awt.datatransfer.DataFlavor
import java.io.{InputStream,OutputStream}
import java.net.{InetSocketAddress,Socket}
import java.util.concurrent.atomic.{AtomicBoolean,AtomicInteger}
import scala.swing.{BoxPanel,Button,Dialog,Font,Frame,Orientation,ScrollPane,Swing,TextArea,TextField,Window}
import scala.swing.event.{ButtonClicked,Key,KeyPressed,KeyReleased,KeyTyped,WindowClosing}

class MainWindow extends Frame {
	val txta = new TextArea(25, 80) {
		peer.setEditable(false)
		peer.setMargin(new Insets(5, 5, 5, 5))
		background = Color.BLACK
		font = new Font(Font.Monospaced, 0, 12)
		foreground = Color.WHITE
	}
	val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
	var host = new StringBuilder("127.0.0.1")
	var port = new StringBuilder("6379")
	var sock = new Socket
	var istream: InputStream = null
	var ostream: OutputStream = null
	var running = new AtomicBoolean(true)
	var idx = new AtomicInteger(0)
	def comm = {
		while(running.get) {
			while(!sock.isClosed && sock.isConnected && 0 < istream.available) {
				val arr: Array[Byte] = new Array[Byte](istream.available)
				istream.read(arr)
				val msg = new String(arr)
				txta.append(msg)
				idx.set(txta.text.length)
				txta.caret.position = idx.get
			}
			if (running.get && sock.isClosed) {
				sock = new Socket
			}
			if (running.get && !sock.isClosed && !sock.isConnected) {
				val cnxDlg = new ConnectDialog(this, sock, running, host, port)
				if (running.get && !sock.isClosed && sock.isConnected) {
					istream = sock.getInputStream
					ostream = sock.getOutputStream
					idx.set(txta.text.length)
				}
			}
		}
	}
	def onDispose = {
		running.set(false)
		sock.close
	}
	contents = new BoxPanel(Orientation.Vertical) {
		border = Swing.EmptyBorder(2)
		contents += new ScrollPane {
			viewportView = txta
		}
	}
	listenTo(txta.keys)
	listenTo(txta.mouse.clicks)
	maximumSize = new Dimension(Short.MaxValue, Short.MaxValue)
	minimumSize = new Dimension(320, 200)
	reactions += {
		case e: KeyReleased => {
			if (e.key == Key.BackSpace && (e.modifiers & 0x80) == 0x80) {
				// delete the last word from textarea
				if (idx.get < txta.text.length) {
					val t = """\s+\S*$""".r.replaceFirstIn(txta.text, "")
					if (idx.get < t.length) {
						txta.text = t
					} else {
						txta.text = txta.text.substring(0, idx.get)
					}
				}
			} else if (e.key == Key.V && (e.modifiers & 0x80) == 0x80) {
				// paste clipboard
				val copied = clipboard.getContents(this)
				if (copied.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					txta.append(copied.getTransferData(DataFlavor.stringFlavor).asInstanceOf[String])
				}
			}
		}
		case e: KeyTyped => {
			if (e.char > 0x1f && e.char < 0x7f || e.char == 0x0a) {
				txta.text += e.char
			} else if (e.char == 0x08) {
				// delete the last character from textarea
				if (idx.get < txta.text.length) {
					txta.text = txta.text.substring(0, txta.text.length - 1)
				}
			}
			if (e.char == 0x0a) {
				val msg = txta.text.substring(idx.get)
				try {
					ostream.write(msg.getBytes)
					ostream.flush
					idx.set(txta.text.length)
				} catch {
					case t: Throwable => sock.close
				}
			}
		}
		case WindowClosing(_) => {
			onDispose
			dispose
		}
	}
	title = "Clinet"
	pack
	centerOnScreen
	open
}

class ConnectDialog(owner: Window, sock: Socket, running: AtomicBoolean, host: StringBuilder, port: StringBuilder) extends Dialog {
	val btn = new Button("Connect") {
		maximumSize = new Dimension(100, 20)
		minimumSize = new Dimension(100, 20)
		preferredSize = new Dimension(100, 20)
	}
	val txtfldAddr = new TextField(host.toString, 16)
	txtfldAddr.caret.position = txtfldAddr.text.length
	val txtfldPort = new TextField(port.toString, 5)
	txtfldPort.caret.position = txtfldPort.text.length
	contents = new BoxPanel(Orientation.Horizontal) {
		border = Swing.EmptyBorder(2)
		contents += txtfldAddr
		contents += txtfldPort
		contents += btn
		modal = true
	}
	btn.requestFocus
	listenTo(btn)
	listenTo(btn.keys)
	listenTo(txtfldAddr.keys)
	listenTo(txtfldPort.keys)
	resizable = false
	reactions += {
		case ButtonClicked(_) | KeyPressed(_, Key.Enter, 0, _) => {
			host.clear
			port.clear
			host ++= txtfldAddr.text
			port ++= txtfldPort.text
			try{
				sock.connect(new InetSocketAddress(txtfldAddr.text, txtfldPort.text.toInt))
			} catch {
				case t: Throwable => {
					sock.close
					Dialog.showMessage(this, "Connection failure", "Error", Dialog.Message.Error)
				}
			}
			dispose
		}
		case WindowClosing(_) => {
			owner.asInstanceOf[MainWindow].onDispose
			owner.dispose
			dispose
		}
	}
	title = "Remote host and port"
	pack
	centerOnScreen
	open
}