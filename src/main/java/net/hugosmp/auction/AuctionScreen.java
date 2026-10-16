package net.hugosmp.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Menü zum Erstellen einer Auktion. */
public class AuctionScreen extends Screen {
    private static final int PW = 244;
    private static final int PH = 230;
    private static final int[] PRESET_SECONDS = {30, 60, 180, 300, 600};
    private static final String[] PRESET_LABELS = {"30 s", "1 Min", "3 Min", "5 Min", "10 Min"};

    private TextFieldWidget secondsField;
    private TextFieldWidget minField;
    private TextFieldWidget buyField;
    private TextFieldWidget payField;
    private ButtonWidget startButton;
    private ItemStack stack = ItemStack.EMPTY;
    private String error = "";

    public AuctionScreen() {
        super(Text.literal("Auktion erstellen"));
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        stack = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack().copy();
        AuctionConfig cfg = HugoAuctionClient.CONFIG;

        String sec = secondsField != null ? secondsField.getText() : String.valueOf(cfg.lastSeconds);
        String min = minField != null ? minField.getText() : cfg.lastMinBid;
        String buy = buyField != null ? buyField.getText() : cfg.lastBuyout;
        String pay = payField != null ? payField.getText() : cfg.payName;

        int left = (width - PW) / 2;
        int top = (height - PH) / 2;

        secondsField = new TextFieldWidget(textRenderer, left + 12, top + 64, 70, 18, Text.literal("Sekunden"));
        secondsField.setMaxLength(6);
        secondsField.setTextPredicate(s -> s.matches("\\d*"));
        secondsField.setText(sec);
        addDrawableChild(secondsField);

        for (int i = 0; i < PRESET_SECONDS.length; i++) {
            final int secs = PRESET_SECONDS[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(PRESET_LABELS[i]), b -> secondsField.setText(String.valueOf(secs)))
                    .dimensions(left + 12 + i * 45, top + 86, 40, 16).build());
        }

        minField = new TextFieldWidget(textRenderer, left + 12, top + 118, 104, 18, Text.literal("Mindestgebot"));
        minField.setMaxLength(16);
        minField.setTextPredicate(s -> s.matches("[0-9.,kKmMbBtT$ ]*"));
        minField.setPlaceholder(Text.literal("z.B. 100k"));
        minField.setText(min);
        addDrawableChild(minField);

        buyField = new TextFieldWidget(textRenderer, left + 128, top + 118, 104, 18, Text.literal("Sofortkauf"));
        buyField.setMaxLength(16);
        buyField.setTextPredicate(s -> s.matches("[0-9.,kKmMbBtT$ ]*"));
        buyField.setPlaceholder(Text.literal("z.B. 1m"));
        buyField.setText(buy);
        addDrawableChild(buyField);

        payField = new TextFieldWidget(textRenderer, left + 12, top + 162, 220, 18, Text.literal("Name"));
        payField.setMaxLength(16);
        payField.setTextPredicate(s -> s.matches("[A-Za-z0-9_]*"));
        payField.setText(pay);
        addDrawableChild(payField);

        boolean running = HugoAuctionClient.MANAGER.isRunning();
        startButton = ButtonWidget.builder(
                        Text.literal("Auktion starten").formatted(Formatting.GREEN, Formatting.BOLD),
                        b -> onStart())
                .dimensions(left + 12, top + 202, 108, 20).build();
        startButton.active = !running;
        addDrawableChild(startButton);

        Text secondLabel = running
                ? Text.literal("Auktion abbrechen").formatted(Formatting.RED)
                : Text.literal("Schließen");
        addDrawableChild(ButtonWidget.builder(secondLabel, b -> {
            if (HugoAuctionClient.MANAGER.isRunning()) HugoAuctionClient.MANAGER.cancel();
            close();
        }).dimensions(left + 124, top + 202, 108, 20).build());
    }

    private void onStart() {
        error = "";
        AuctionConfig cfg = HugoAuctionClient.CONFIG;

        if (stack.isEmpty()) {
            error = "Halte ein Item in der Hand!";
            return;
        }
        int secs;
        try {
            secs = Integer.parseInt(secondsField.getText().trim());
        } catch (NumberFormatException e) {
            error = "Bitte Sekunden eingeben";
            return;
        }
        if (secs < 5 || secs > 86400) {
            error = "Dauer: 5 bis 86400 Sekunden";
            return;
        }
        String minText = minField.getText().trim();
        String buyText = buyField.getText().trim();
        double min = 0;
        double buy = 0;
        if (!minText.isEmpty()) {
            min = Fmt.parse(minText);
            if (Double.isNaN(min)) {
                error = "Mindestgebot ungültig";
                return;
            }
        }
        if (!buyText.isEmpty()) {
            buy = Fmt.parse(buyText);
            if (Double.isNaN(buy)) {
                error = "Sofortkauf ungültig";
                return;
            }
        }
        if (buy > 0 && buy < min) {
            error = "Sofortkauf muss mindestens so hoch sein wie das Mindestgebot";
            return;
        }
        String pay = payField.getText().trim();
        if (!pay.isEmpty()) cfg.payName = pay;
        cfg.lastSeconds = secs;
        cfg.lastMinBid = minText;
        cfg.lastBuyout = buyText;
        cfg.save();

        if (HugoAuctionClient.MANAGER.start(stack, secs, min, buy)) close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int left = (width - PW) / 2;
        int top = (height - PH) / 2;
        int cx = left + PW / 2;

        // Panel
        Gfx.rect(ctx, left - 1, top - 1, left + PW + 1, top + PH + 1, 0xFFD4A017);
        Gfx.rect(ctx, left, top, left + PW, top + PH, 0xF2101018);
        for (int i = 0; i < 22; i++) {
            int c = Gfx.lerpColor(0x40300A, 0x1A1408, i / 21.0);
            int inset = i == 0 ? 1 : 0;
            ctx.fill(left + inset, top + i, left + PW - inset, top + i + 1, Gfx.argb(255, c));
        }
        Gfx.centered(ctx, textRenderer, "AUKTION ERSTELLEN", cx, top + 7, 0xFFFFD34D, true);

        // Item
        if (stack.isEmpty()) {
            Gfx.text(ctx, textRenderer, "Halte das Item in der Hand, das du versteigern willst.",
                    left + 12, top + 32, 0xFFFF5555, false);
        } else {
            Gfx.scaledItem(ctx, stack, left + 12, top + 25, 1.25f);
            String name = stack.getName().getString();
            Gfx.text(ctx, textRenderer, Gfx.trunc(textRenderer, name, PW - 56, true), left + 40, top + 28, 0xFFFFFFFF, true);
            Gfx.text(ctx, textRenderer, "Menge: " + stack.getCount(), left + 40, top + 39, 0xFF9999AA, false);
        }

        // Beschriftungen
        Gfx.text(ctx, textRenderer, "Dauer in Sekunden", left + 12, top + 53, 0xFFCCCCCC, false);
        double secs = parseSeconds();
        if (secs > 0) {
            Gfx.text(ctx, textRenderer, "= " + Fmt.time((long) secs) + " Min", left + 90, top + 69, 0xFF55FF55, true);
        }
        Gfx.text(ctx, textRenderer, "Mindestgebot", left + 12, top + 107, 0xFFCCCCCC, false);
        Gfx.text(ctx, textRenderer, "Sofortkauf", left + 128, top + 107, 0xFFCCCCCC, false);
        Gfx.centered(ctx, textRenderer, "z.B. 100k, 1,5m  |  leer = aus", cx, top + 140, 0xFF777788, false);
        Gfx.text(ctx, textRenderer, "Zahlungen gehen an (Anzeige: /pay NAME)", left + 12, top + 151, 0xFFCCCCCC, false);

        // Vorschau oder Fehler
        if (!error.isEmpty()) {
            Gfx.centered(ctx, textRenderer, error, cx, top + 188, 0xFFFF5555, true);
        } else {
            double min = Fmt.parse(minField == null ? "" : minField.getText());
            double buy = Fmt.parse(buyField == null ? "" : buyField.getText());
            String pm = Double.isNaN(min) || min <= 0 ? "kein Mindestgebot" : "Min. " + Fmt.money(min);
            String pb = Double.isNaN(buy) || buy <= 0 ? "kein Sofortkauf" : "Sofortkauf " + Fmt.money(buy);
            Gfx.centered(ctx, textRenderer, pm + "  |  " + pb, cx, top + 188, 0xFFAAAAAA, false);
        }
    }

    private double parseSeconds() {
        if (secondsField == null) return 0;
        try {
            return Integer.parseInt(secondsField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
