/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import net.runelite.client.ui.PluginPanel;

public class ChallengePanel extends PluginPanel {
    private final JTextArea body = new JTextArea("2005Scape\n22 June 2005\n\nLog in to view your challenge.");
    private final JScrollPane scroll = new JScrollPane(body);
    private final JComboBox<ReceiptChoice> receipts = new JComboBox<>();
    private final JTextField source = new JTextField();
    private static final class ReceiptChoice {
        final int id; final String name;
        ReceiptChoice(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    public ChallengePanel(java.util.function.BiConsumer<Integer, String> confirm, java.util.function.IntConsumer decline) {
        setLayout(new BorderLayout()); body.setEditable(false); body.setLineWrap(true); body.setWrapStyleWord(true);
        body.setBackground(new Color(62, 52, 40)); body.setForeground(new Color(255, 210, 96));
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); add(scroll, BorderLayout.CENTER);
        JPanel review = new JPanel(); review.setLayout(new BoxLayout(review, BoxLayout.Y_AXIS));
        review.add(new JLabel("New receipts needing source review")); review.add(receipts);
        review.add(new JLabel("Historical source you checked:")); review.add(source);
        JButton accept = new JButton("Confirm historical source");
        accept.addActionListener(event -> {
            ReceiptChoice choice = (ReceiptChoice) receipts.getSelectedItem();
            if (choice != null && !source.getText().trim().isEmpty()) { confirm.accept(choice.id, source.getText()); source.setText(""); }
        });
        JButton reject = new JButton("Keep restricted");
        reject.addActionListener(event -> {
            ReceiptChoice choice = (ReceiptChoice) receipts.getSelectedItem();
            if (choice != null) { decline.accept(choice.id); }
        });
        review.add(accept); review.add(reject); add(review, BorderLayout.SOUTH);
    }
    void update(RuleBook rules, ChallengeState state, String notice, String advice) {
        StringBuilder text = new StringBuilder("2005Scape\n22 June 2005 inclusive\n\n");
        text.append("Started: ").append(state.started).append("\nMissed loot: ").append(String.format("%,d", state.missedValue))
            .append(" gp\nValuation: ").append(state.marketValue ? "cached GE" : "High Alchemy")
            .append("\n\n").append(notice).append("\n\nSLAYER\n").append(advice).append("\n\n");
        List<String> groups = rules.goals().stream().map(goal -> goal.group).distinct().collect(Collectors.toList());
        for (String group : groups) {
            List<RuleBook.Goal> goals = rules.goals().stream().filter(goal -> goal.group.equals(group)).collect(Collectors.toList());
            long complete = goals.stream().filter(goal -> goal.complete(state.acquired)).count();
            text.append(group.toUpperCase(java.util.Locale.ROOT)).append(" ").append(complete).append("/").append(goals.size()).append("\n");
            for (RuleBook.Goal goal : goals) {
                text.append(goal.complete(state.acquired) ? "[x] " : "[ ] ").append(goal.name).append("\n");
                if (goal.notes != null && !goal.notes.isEmpty()) { text.append("    ").append(goal.notes).append("\n"); }
            }
            text.append("\n");
        }
        text.append("DATA COVERAGE\n").append(rules.items().size()).append(" item records; ").append(rules.eligibleCount())
            .append(" eligible IDs.\nSnapshot: August 2021. Missing/new IDs remain unverified.\n\n")
            .append("Progress requires a new receipt and a recognised historical source: loot, listed shops, quest rewards or production recipes. Unattributed receipts are not credited.\n")
            .append("Quarantined item types: ").append(state.quarantined.size()).append("\n")
            .append("Unknown new sources are held for review. Confirm only an acquisition from a source that existed by the cutoff. Confirmed modern receipts cannot be overridden here.\n")
            .append("Your checklist is configured in goal-definitions.json.\n");
        String value = text.toString();
        List<ReceiptChoice> pending = state.pendingReceipts.keySet().stream().sorted().map(id ->
            new ReceiptChoice(id, rules.item(id).name + " (" + id + ")")).collect(Collectors.toList());
        SwingUtilities.invokeLater(() -> {
            if (!body.getText().equals(value)) {
                int caret = body.getCaretPosition(); int position = scroll.getVerticalScrollBar().getValue();
                body.setText(value); body.setCaretPosition(Math.min(caret, value.length()));
                scroll.getVerticalScrollBar().setValue(position);
            }
            ReceiptChoice selected = (ReceiptChoice) receipts.getSelectedItem();
            receipts.removeAllItems(); pending.forEach(receipts::addItem);
            if (selected != null) {
                pending.stream().filter(choice -> choice.id == selected.id).findFirst().ifPresent(receipts::setSelectedItem);
            }
        });
    }
}
