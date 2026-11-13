package models;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class ChatDocumentLimit extends PlainDocument {
    private final int limite;

    public ChatDocumentLimit(int limite) {
        super();
        this.limite = limite;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) {
            return;
        }

        // Se a quantidade atual de caracteres + a nova string for menor ou igual ao limite, insere
        if ((getLength() + str.length()) <= limite) {
            super.insertString(offset, str, attr);
        } else {
            // Opcional: calcula quantos caracteres ainda cabem antes de travar
            int caracteresRestantes = limite - getLength();
            if (caracteresRestantes > 0) {
                super.insertString(offset, str.substring(0, caracteresRestantes), attr);
            }
        }
    }
}
