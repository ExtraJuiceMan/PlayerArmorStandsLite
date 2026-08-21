package com.danrus.pas.render.item;

import com.danrus.pas.api.NameInfo;
import com.danrus.pas.data.cape.CapeData;
import com.danrus.pas.data.skin.SkinData;
import org.jetbrains.annotations.Nullable;

public record ItemRenderData(SkinData skinData, @Nullable CapeData capeData, NameInfo info) {}
